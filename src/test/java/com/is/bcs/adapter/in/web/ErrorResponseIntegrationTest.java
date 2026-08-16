package com.is.bcs.adapter.in.web;

import com.is.bcs.domain.member.exception.InvalidMemberStateException;
import com.is.bcs.domain.member.exception.MemberNotFoundException;
import jakarta.servlet.RequestDispatcher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;


import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 에러 응답 표준(RFC 9457) 통합 검증 — 실제 컨텍스트의 자동구성 JsonMapper로
 * code·timestamp·errors가 top-level로 직렬화되는 계약을 확인한다. (DB 필요: bcs/docker-compose)
 */
@SpringBootTest
@Import(ErrorResponseIntegrationTest.ErrorTriggerController.class)
class ErrorResponseIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private String bodyOf(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("미매핑 경로 404 — fallback code·번들 detail·KST timestamp가 top-level로 실린다")
    void unknownPath_fallbackProblemDetail() throws Exception {
        MvcResult result = mockMvc.perform(get("/unknown-path"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        String body = bodyOf(result);
        assertTrue(body.contains("\"code\":\"COMMON_BAD_REQUEST\""));
        assertTrue(body.contains("요청한 리소스를 찾을 수 없습니다")); // 프레임워크 원문이 아니라 번들 문구
        assertTrue(body.contains("+09:00"));
        assertFalse(body.contains("\"properties\"")); // 확장 멤버가 중첩되지 않고 평탄화됐는지
    }

    @Test
    @DisplayName("도메인 예외 404 — MemberErrorCode와 예외 메시지가 실린다")
    void domainNotFound_memberErrorCode() throws Exception {
        MvcResult result = mockMvc.perform(get("/test-errors/not-found"))
                .andExpect(status().isNotFound())
                .andReturn();

        String body = bodyOf(result);
        assertTrue(body.contains("\"code\":\"MEMBER_NOT_FOUND\""));
        assertTrue(body.contains("회원을 찾을 수 없습니다: 1"));
        assertTrue(body.contains("+09:00"));
    }

    @Test
    @DisplayName("도메인 상태 전이 위반 — 422 MEMBER_INVALID_STATE")
    void domainInvalidState_422() throws Exception {
        MvcResult result = mockMvc.perform(get("/test-errors/invalid-state"))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertTrue(bodyOf(result).contains("\"code\":\"MEMBER_INVALID_STATE\""));
    }

    @Test
    @DisplayName("@Valid 바디 검증 실패 — COMMON_INVALID_INPUT과 errors[] 필드 목록")
    void bodyValidation_errorsArray() throws Exception {
        MvcResult result = mockMvc.perform(post("/test-errors/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        String body = bodyOf(result);
        assertTrue(body.contains("\"code\":\"COMMON_INVALID_INPUT\""));
        assertTrue(body.contains("\"errors\":"));
        assertTrue(body.contains("\"field\":\"name\""));
        assertTrue(body.contains("이름은 필수입니다."));
    }

    @Test
    @DisplayName("파라미터 제약 위반(내장 메서드 검증) — COMMON_INVALID_INPUT + errors[] 파라미터명")
    void parameterValidation_errorsArray() throws Exception {
        MvcResult result = mockMvc.perform(get("/test-errors/param-validate").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andReturn();

        String body = bodyOf(result);
        assertTrue(body.contains("\"code\":\"COMMON_INVALID_INPUT\""));
        assertTrue(body.contains("\"field\":\"page\""));
    }

    @Test
    @DisplayName("파라미터 타입 변환 실패 — fallback이 아니라 COMMON_INVALID_INPUT + errors[]")
    void typeMismatch_invalidInput() throws Exception {
        MvcResult result = mockMvc.perform(get("/test-errors/type-mismatch").param("count", "abc"))
                .andExpect(status().isBadRequest())
                .andReturn();

        String body = bodyOf(result);
        assertTrue(body.contains("\"code\":\"COMMON_INVALID_INPUT\""));
        assertTrue(body.contains("\"field\":\"count\""));
    }

    @Test
    @DisplayName("미처리 예외 500 — 원인 메시지는 응답에 노출되지 않는다")
    void unexpectedException_hidesCause() throws Exception {
        MvcResult result = mockMvc.perform(get("/test-errors/boom"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        String body = bodyOf(result);
        assertTrue(body.contains("\"code\":\"COMMON_INTERNAL_ERROR\""));
        assertTrue(body.contains("서버 내부 오류가 발생했습니다"));
        assertFalse(body.contains("의도된 테스트 예외")); // 내부 원인 비노출
    }

    @Test
    @DisplayName("/error 직접 진입(필터단 우회 경로) — ProblemDetail로 정규화된다")
    void errorPath_normalized() throws Exception {
        MvcResult result = mockMvc.perform(get("/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        String body = bodyOf(result);
        assertTrue(body.contains("\"code\":\"COMMON_INTERNAL_ERROR\""));
        assertTrue(body.contains("서버 내부 오류가 발생했습니다"));
    }

    /** 에러 경로를 실제로 태우기 위한 테스트 전용 컨트롤러 */
    @RestController
    @RequestMapping("/test-errors")
    static class ErrorTriggerController {

        @GetMapping("/not-found")
        String notFound() {
            throw new MemberNotFoundException("회원을 찾을 수 없습니다: 1");
        }

        @GetMapping("/invalid-state")
        String invalidState() {
            throw new InvalidMemberStateException("승인 대기 상태에서만 처리할 수 있습니다.");
        }

        @PostMapping("/validate")
        String validate(@Valid @RequestBody ProfileRequest request) {
            return "ok";
        }

        @GetMapping("/type-mismatch")
        String typeMismatch(@RequestParam("count") int count) {
            return "ok:" + count;
        }

        // 클래스에 @Validated 없이 파라미터 제약만 — Spring 6.1+ 내장 메서드 검증 경로(컨벤션)
        @GetMapping("/param-validate")
        String paramValidate(@RequestParam("page") @Min(1) int page) {
            return "ok:" + page;
        }

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("의도된 테스트 예외");
        }
    }

    record ProfileRequest(@NotBlank(message = "이름은 필수입니다.") String name) {
    }

    @Test
    @DisplayName("/error 401 — AUTHENTICATION_REQUIRED")
    void unauthorized_normalized() throws Exception {
        mockMvc.perform(
                        get("/error")
                                .requestAttr(
                                        RequestDispatcher.ERROR_STATUS_CODE,
                                        HttpStatus.UNAUTHORIZED.value()
                                )
                                .requestAttr(
                                        RequestDispatcher.ERROR_REQUEST_URI,
                                        "/api/survey-projects"
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("/error 403 — AUTH_FORBIDDEN")
    void forbidden_normalized() throws Exception {
        mockMvc.perform(
                        get("/error")
                                .requestAttr(
                                        RequestDispatcher.ERROR_STATUS_CODE,
                                        HttpStatus.FORBIDDEN.value()
                                )
                                .requestAttr(
                                        RequestDispatcher.ERROR_REQUEST_URI,
                                        "/api/admin/members"
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }
}
