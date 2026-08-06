package com.is.bcs.adapter.in.web.chat;

import com.is.bcs.adapter.in.security.jwt.AccessTokenAuthenticationFilter;
import com.is.bcs.adapter.in.web.exception.ErrorDetailResolver;
import com.is.bcs.application.port.in.chat.AskChatBotUseCase;
import com.is.bcs.config.TimeConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** 챗봇 API 계약 검증 — 모델 없이 유스케이스 페이크로 컨트롤러·검증만 본다. */
// 인증은 이 조각의 관심사가 아니다 — 컨트롤러 조각은 서블릿 필터·보안 설정을 함께 세우는데,
// 그 판이 서려면 토큰 검증기부터 OAuth 클라이언트까지 딸려 와야 해서 컨트롤러를 보기도 전에 무너진다.
// 인증이 붙은 실제 사슬은 전체를 띄우는 API 시험(SurveyApiTest 등)이 본다.
@WebMvcTest(controllers = ChatController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
        OAuth2ClientWebSecurityAutoConfiguration.class
}, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = AccessTokenAuthenticationFilter.class))
@Import({ChatApiTest.FakeChatBot.class, ErrorDetailResolver.class, TimeConfig.class})
class ChatApiTest {

    @Autowired
    private MockMvc mockMvc;

    private MvcResult chat(String body) throws Exception {
        return mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private String bodyOf(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("질문을 보내면 200과 답변을 돌려준다")
    void chat_returnsAnswer() throws Exception {
        MvcResult result = chat("{\"message\": \"기준점 몇 개야?\"}");

        assertEquals(200, result.getResponse().getStatus());
        assertTrue(bodyOf(result).contains("\"answer\":\"질문 받음: 기준점 몇 개야?\""));
    }

    @Test
    @DisplayName("빈 질문은 400 COMMON_INVALID_INPUT과 errors[]로 거부한다")
    void chat_blankMessage_returns400() throws Exception {
        MvcResult result = chat("{\"message\": \"  \"}");

        assertEquals(400, result.getResponse().getStatus());
        String body = bodyOf(result);
        assertTrue(body.contains("\"code\":\"COMMON_INVALID_INPUT\""));
        assertTrue(body.contains("\"errors\""));
        // errors[]에 위반 필드와 검증 메시지가 실렸는지 — 실제 문구로 확인(top-level message 필드는 없음)
        assertTrue(body.contains("\"field\":\"message\""));
        assertTrue(body.contains("질문은 필수입니다"));
    }

    /** 유스케이스 페이크 — 질문이 컨트롤러를 통과해 그대로 전달되는지만 본다. */
    @TestConfiguration
    static class FakeChatBot {

        @Bean
        AskChatBotUseCase askChatBotUseCase() {
            return question -> "질문 받음: " + question;
        }
    }
}
