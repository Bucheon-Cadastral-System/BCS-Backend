package com.is.bcs.adapter.in.web.exception;

import com.is.bcs.config.TimeConfig;
import com.is.bcs.domain.survey.exception.InvalidSurveyException;
import com.is.bcs.domain.survey.exception.SurveyRecordNotFoundException;
import com.is.bcs.domain.token.exception.ExpiredOAuthExchangeCodeException;
import com.is.bcs.domain.token.exception.ExpiredTokenException;
import com.is.bcs.domain.token.exception.InvalidOAuthExchangeCodeException;
import com.is.bcs.domain.token.exception.InvalidTokenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 도메인 예외 → 응답(status·code) 매핑 검증.
 * 이 매핑이 곧 프론트가 분기하는 계약이라, 코드와 상태가 조용히 바뀌지 않게 고정한다.
 */
class GlobalExceptionHandlerTest {

    private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(
            Clock.fixed(NOW, TimeConfig.KST), new ErrorDetailResolver(new StaticMessageSource()));

    private void assertMapped(ProblemDetail problem, HttpStatus status, ErrorCode expected, String detail) {
        assertEquals(status.value(), problem.getStatus());
        assertNotNull(problem.getProperties());
        assertEquals(expected.code(), problem.getProperties().get("code"));
        assertEquals(detail, problem.getDetail());
        // 실패 응답에는 항상 발생 시각이 실린다(KST)
        assertEquals(OffsetDateTime.ofInstant(NOW, TimeConfig.KST), problem.getProperties().get("timestamp"));
    }

    @Test
    @DisplayName("조사 도메인 예외 — 404·422 로 매핑된다")
    void surveyExceptions() {
        assertMapped(handler.handleSurveyRecordNotFound(new SurveyRecordNotFoundException("기록 없음")),
                HttpStatus.NOT_FOUND, SurveyErrorCode.SURVEY_RECORD_NOT_FOUND, "기록 없음");
        assertMapped(handler.handleInvalidSurvey(new InvalidSurveyException("조사명 없음")),
                HttpStatus.BAD_REQUEST, SurveyErrorCode.SURVEY_INVALID, "조사명 없음");
    }

    @Test
    @DisplayName("페이지·커서 요청 오류 — 400 으로 매핑되고 서버 문장이 아니라 안내 문구가 실린다")
    void pagingExceptions() {
        // 받는 핸들러가 없으면 미처리 예외로 새어 500 이 된다. 잘못 보낸 요청이므로 400 이어야 한다
        assertMapped(handler.handleInvalidPageRequest(new InvalidPageRequestException("page는 0 이상이어야 합니다. 입력값=-1")),
                HttpStatus.BAD_REQUEST, CommonErrorCode.PAGE_REQUEST_INVALID,
                "목록을 불러올 수 없습니다. 화면을 새로고침해 주세요.");
        assertMapped(handler.handleInvalidCursor(new InvalidCursorException()),
                HttpStatus.BAD_REQUEST, CommonErrorCode.CURSOR_INVALID,
                "목록을 이어 불러올 수 없습니다. 화면을 새로고침해 주세요.");
    }

    @Test
    @DisplayName("토큰·교환코드 예외 — 각각의 보안 코드로 매핑된다")
    void tokenExceptions() {
        assertMapped(handler.handleExpiredToken(new ExpiredTokenException("만료", new IllegalStateException())),
                SecurityErrorCode.TOKEN_EXPIRED.status(), SecurityErrorCode.TOKEN_EXPIRED, "만료");
        assertMapped(handler.handleInvalidToken(new InvalidTokenException("위조")),
                SecurityErrorCode.TOKEN_INVALID.status(), SecurityErrorCode.TOKEN_INVALID, "위조");
        assertMapped(handler.handleExpiredOAuthExchangeCode(new ExpiredOAuthExchangeCodeException("코드 만료")),
                SecurityErrorCode.OAUTH_EXCHANGE_CODE_EXPIRED.status(),
                SecurityErrorCode.OAUTH_EXCHANGE_CODE_EXPIRED, "코드 만료");
        assertMapped(handler.handleInvalidOAuthExchangeCode(new InvalidOAuthExchangeCodeException("코드 위조")),
                SecurityErrorCode.OAUTH_EXCHANGE_CODE_INVALID.status(),
                SecurityErrorCode.OAUTH_EXCHANGE_CODE_INVALID, "코드 위조");
    }

    @Test
    @DisplayName("중복 저장은 409 로 내리고 제약 이름·SQL 은 응답에 싣지 않는다")
    void duplicateViolation() {
        ProblemDetail problem = handler.handleDataIntegrityViolation(new DataIntegrityViolationException(
                "uk_control_points_point_no 위반: insert into ...",
                new SQLException("duplicate key", "23505")));

        assertMapped(problem, HttpStatus.CONFLICT, CommonErrorCode.COMMON_CONFLICT,
                "이미 등록된 값이라 저장할 수 없습니다.");
    }

    @Test
    @DisplayName("원인 예외가 없는 중복 예외도 409 다 — 상태 코드를 볼 수 없으면 타입으로 가른다")
    void duplicateKeyExceptionWithoutCause() {
        ProblemDetail problem = handler.handleDataIntegrityViolation(
                new DuplicateKeyException("uk_control_points_point_no"));

        assertMapped(problem, HttpStatus.CONFLICT, CommonErrorCode.COMMON_CONFLICT,
                "이미 등록된 값이라 저장할 수 없습니다.");
    }

    @Test
    @DisplayName("배치 저장 실패도 409 다 — 진짜 원인이 cause 가 아니라 다음 예외 사슬에 담긴다")
    void batchedDuplicateViolation() {
        BatchUpdateException batch = new BatchUpdateException("batch entry 3 failed", new int[0]);
        batch.setNextException(new SQLException("duplicate key", "23505"));

        ProblemDetail problem = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("could not execute batch", batch));

        assertMapped(problem, HttpStatus.CONFLICT, CommonErrorCode.COMMON_CONFLICT,
                "이미 등록된 값이라 저장할 수 없습니다.");
    }

    @Test
    @DisplayName("서로를 가리키는 예외 사슬에서도 멈춘다")
    void selfReferencingCause() {
        SQLException looping = new SQLException("무결성 위반", "23000");
        looping.setNextException(looping);

        ProblemDetail problem = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("제약 위반", looping));

        assertMapped(problem, HttpStatus.INTERNAL_SERVER_ERROR, CommonErrorCode.COMMON_INTERNAL_ERROR,
                "서버 내부 오류가 발생했습니다");
    }

    @Test
    @DisplayName("참조 제약 위반은 409 다 — 사전 검증 뒤 참조 대상이 바뀐 경합이라 다시 시도하면 풀린다")
    void foreignKeyViolation() {
        // 23503 = foreign_key_violation
        ProblemDetail problem = handler.handleDataIntegrityViolation(new DataIntegrityViolationException(
                "fk_survey_targets_project 위반", new SQLException("no referenced row", "23503")));

        assertMapped(problem, HttpStatus.CONFLICT, CommonErrorCode.COMMON_CONFLICT,
                "다른 작업과 겹쳐 처리하지 못했습니다. 다시 시도해 주세요.");
    }

    @Test
    @DisplayName("중복·참조가 아닌 제약 위반은 500 이다 — 도메인 검증이 빠진 자리를 정상 응답으로 감추지 않는다")
    void otherIntegrityViolation() {
        // 23502 = not_null_violation — 필수값이 비었다는 건 경합이 아니라 검증 누락이다
        ProblemDetail problem = handler.handleDataIntegrityViolation(new DataIntegrityViolationException(
                "not-null 위반", new SQLException("null value in column", "23502")));

        assertMapped(problem, HttpStatus.INTERNAL_SERVER_ERROR, CommonErrorCode.COMMON_INTERNAL_ERROR,
                "서버 내부 오류가 발생했습니다");
    }

    @Test
    @DisplayName("예상하지 못한 예외는 원인을 감추고 500 일반 문구로 내린다")
    void unexpectedException() {
        ProblemDetail problem = handler.handleUnexpected(new IllegalStateException("커넥션 풀 고갈"));

        assertMapped(problem, HttpStatus.INTERNAL_SERVER_ERROR, CommonErrorCode.COMMON_INTERNAL_ERROR,
                "서버 내부 오류가 발생했습니다");
    }

    @Test
    @DisplayName("에러 코드 상수명이 곧 응답 code 값이다 — 오타가 나면 프론트 분기가 조용히 깨진다")
    void errorCodeNameIsResponseCode() {
        for (ErrorCode code : allErrorCodes()) {
            assertEquals(((Enum<?>) code).name(), code.code());
            assertNotNull(code.status());
        }
    }

    private static ErrorCode[] allErrorCodes() {
        return java.util.stream.Stream.of(
                        CommonErrorCode.values(), SecurityErrorCode.values(),
                        SurveyErrorCode.values(), ControlPointErrorCode.values(), MemberErrorCode.values())
                .flatMap(java.util.Arrays::stream)
                .toArray(ErrorCode[]::new);
    }
}
