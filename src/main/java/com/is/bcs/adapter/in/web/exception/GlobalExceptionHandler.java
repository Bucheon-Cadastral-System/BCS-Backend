package com.is.bcs.adapter.in.web.exception;

import com.is.bcs.domain.controlpoint.exception.ControlPointInUseException;
import com.is.bcs.domain.controlpoint.exception.ControlPointNotFoundException;
import com.is.bcs.domain.controlpoint.exception.DuplicateControlPointException;
import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import com.is.bcs.domain.member.exception.*;
import com.is.bcs.domain.survey.exception.InvalidSurveyException;
import com.is.bcs.domain.survey.exception.SurveyProjectNotFoundException;
import com.is.bcs.domain.survey.exception.SurveyRecordNotFoundException;
import com.is.bcs.domain.survey.exception.SurveyTargetNotFoundException;
import com.is.bcs.domain.token.exception.ExpiredOAuthExchangeCodeException;
import com.is.bcs.domain.token.exception.ExpiredTokenException;
import com.is.bcs.domain.token.exception.InvalidOAuthExchangeCodeException;
import com.is.bcs.domain.token.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 모든 실패 응답을 ProblemDetail(RFC 9457) + code + timestamp로 통일하는 전역 예외 처리기.
 *
 * problemdetails 활성화 시 Boot가 ResponseEntityExceptionHandler 기반 내장 핸들러를 등록하므로,
 * 프레임워크 표준 예외까지 같은 포맷으로 내리려면 이 클래스를 상속해 훅을 오버라이드해야 한다
 * (비상속 @ExceptionHandler는 내장 핸들러에 우선순위가 밀려 무시된다).
 *
 * 서블릿 필터단(보안 401/403 등)에서 난 예외는 이 advice의 범위 밖이라 별도 핸들러로 처리한다.
 */
@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final Clock clock;
    private final ErrorDetailResolver errorDetailResolver;

    /**
     * 프레임워크 예외 공통 경유 지점 — code(구체 핸들러가 안 정했을 때만 fallback)와 timestamp를 일괄 보강한다.
     * fallback 응답은 detail도 번들 문구로 통일한다(/error 정규화와 동일 문구) —
     * 프레임워크 원문은 버전 따라 바뀌는 영문이라 응답엔 싣지 않고 로그로만 남긴다.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response =
                super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            var props = problem.getProperties();
            if (props == null || !props.containsKey("code")) {
                problem.setProperty("code", defaultCode(statusCode));
                problem.setDetail(errorDetailResolver.detailFor(statusCode));
                if (statusCode.is5xxServerError()) {
                    log.error("프레임워크 예외(5xx fallback)", ex);
                } else {
                    log.debug("프레임워크 예외(4xx fallback): {}", ex.getMessage());
                }
            }
            problem.setProperty("timestamp", OffsetDateTime.now(clock));
        }
        return response;
    }

    /** 요청 바디(@Valid) 검증 실패 — detail 요약 + errors[] 필드별 목록 */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String detail = ex.getBindingResult().getAllErrors().stream()
                .map(e -> e instanceof FieldError fe
                        ? fe.getField() + ": " + e.getDefaultMessage()
                        : e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        List<ValidationError> errors = ex.getBindingResult().getAllErrors().stream()
                .map(e -> e instanceof FieldError fe
                        ? new ValidationError(fe.getField(), e.getDefaultMessage())
                        : new ValidationError(null, e.getDefaultMessage()))
                .toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", CommonErrorCode.COMMON_INVALID_INPUT.code());
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /** 파라미터·경로변수 제약 위반 — status가 가변(파라미터 400 / 반환값 500)이라 status에 맞는 code를 고른다. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (status.is5xxServerError()) { // 반환값 검증 실패 = 서버 결함 → 필드·파라미터명 비노출
            log.error("메서드 반환값 검증 실패", ex);
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "서버 내부 오류가 발생했습니다");
            problem.setProperty("code", CommonErrorCode.COMMON_INTERNAL_ERROR.code());
            return handleExceptionInternal(ex, problem, headers, status, request);
        }
        List<ValidationError> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(err -> new ValidationError(
                                result.getMethodParameter().getParameterName(), err.getDefaultMessage())))
                .toList();
        String detail = errors.stream()
                .map(e -> (e.field() != null ? e.field() + ": " : "") + e.message())
                .collect(Collectors.joining(", "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", CommonErrorCode.COMMON_INVALID_INPUT.code());
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /** 파라미터·경로변수 타입 변환 실패(숫자 자리에 문자 등) — 미분류 fallback이 아니라 입력 오류로 분류한다. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String field = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
        String message = "올바른 형식이 아닙니다";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, (field != null ? field + ": " : "") + message);
        problem.setProperty("code", CommonErrorCode.COMMON_INVALID_INPUT.code());
        problem.setProperty("errors", List.of(new ValidationError(field, message)));
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    // ── 도메인 예외 ──────────────────────────────────────────────

    @ExceptionHandler(MemberNotFoundException.class)
    public ProblemDetail handleMemberNotFound(MemberNotFoundException e) {
        return problem(MemberErrorCode.MEMBER_NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidMemberStateException.class)
    public ProblemDetail handleInvalidMemberState(InvalidMemberStateException e) {
        return problem(MemberErrorCode.MEMBER_INVALID_STATE, e.getMessage());
    }

    @ExceptionHandler(InvalidMemberProfileException.class)
    public ProblemDetail handleInvalidMemberProfile(InvalidMemberProfileException e) {
        return problem(MemberErrorCode.MEMBER_PROFILE_INVALID, e.getMessage());
    }

    @ExceptionHandler(ControlPointNotFoundException.class)
    public ProblemDetail handleControlPointNotFound(ControlPointNotFoundException e) {
        return problem(ControlPointErrorCode.CONTROL_POINT_NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidControlPointException.class)
    public ProblemDetail handleInvalidControlPoint(InvalidControlPointException e) {
        return problem(ControlPointErrorCode.CONTROL_POINT_INVALID, e.getMessage());
    }

    @ExceptionHandler(DuplicateControlPointException.class)
    public ProblemDetail handleDuplicateControlPoint(DuplicateControlPointException e) {
        return problem(ControlPointErrorCode.CONTROL_POINT_DUPLICATE, e.getMessage());
    }

    @ExceptionHandler(ControlPointInUseException.class)
    public ProblemDetail handleControlPointInUse(ControlPointInUseException e) {
        return problem(ControlPointErrorCode.CONTROL_POINT_IN_USE, e.getMessage());
    }

    /**
     * 저장 제약 위반 — 중복만 409 로 알린다.
     * 외래키·필수값·CHECK 위반까지 409 로 뭉개면 도메인 검증이 빠진 자리를 정상 응답처럼 감춘다.
     * 제약 이름·SQL 은 사용자에게 뜻이 없고 노출하면 스키마가 드러나므로 어느 쪽이든 로그로만 남긴다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException e) {
        if (isUniqueViolation(e)) {
            log.warn("중복 저장 시도", e);
            return problem(CommonErrorCode.COMMON_CONFLICT, "이미 등록된 값이라 저장할 수 없습니다.");
        }
        log.error("저장 제약 위반", e);
        return problem(CommonErrorCode.COMMON_INTERNAL_ERROR, "서버 내부 오류가 발생했습니다");
    }

    /** SQL 표준의 unique_violation. 제약 이름 규칙에 기대지 않으려 상태 코드로 판단한다. */
    private static final String UNIQUE_VIOLATION = "23505";

    /**
     * 중복 저장으로 실패한 것인지.
     * 원인 예외가 없는 DuplicateKeyException 은 상태 코드를 볼 수 없으므로 타입으로 먼저 가른다.
     * 배치로 묶어 저장하면 진짜 원인이 cause 가 아니라 SQLException 의 다음 예외 사슬에 담기므로 둘 다 훑는다.
     */
    private static boolean isUniqueViolation(DataIntegrityViolationException e) {
        if (e instanceof DuplicateKeyException) {
            return true;
        }
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Throwable> pending = new ArrayDeque<>(List.of(e));
        while (!pending.isEmpty()) {
            Throwable current = pending.pop();
            if (!visited.add(current)) {
                continue; // 예외가 서로를 가리켜도 멈춘다
            }
            if (current instanceof SQLException sql) {
                if (UNIQUE_VIOLATION.equals(sql.getSQLState())) {
                    return true;
                }
                pushIfPresent(pending, sql.getNextException());
            }
            pushIfPresent(pending, current.getCause());
        }
        return false;
    }

    private static void pushIfPresent(Deque<Throwable> pending, Throwable candidate) {
        if (candidate != null) {
            pending.push(candidate);
        }
    }

    @ExceptionHandler(SurveyProjectNotFoundException.class)
    public ProblemDetail handleSurveyProjectNotFound(SurveyProjectNotFoundException e) {
        return problem(SurveyErrorCode.SURVEY_PROJECT_NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(SurveyRecordNotFoundException.class)
    public ProblemDetail handleSurveyRecordNotFound(SurveyRecordNotFoundException e) {
        return problem(SurveyErrorCode.SURVEY_RECORD_NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(SurveyTargetNotFoundException.class)
    public ProblemDetail handleSurveyTargetNotFound(SurveyTargetNotFoundException e) {
        return problem(SurveyErrorCode.SURVEY_TARGET_NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidSurveyException.class)
    public ProblemDetail handleInvalidSurvey(InvalidSurveyException e) {
        return problem(SurveyErrorCode.SURVEY_INVALID, e.getMessage());
    }

    @ExceptionHandler(ExpiredTokenException.class)
    public ProblemDetail handleExpiredToken(ExpiredTokenException e) {
        return problem(SecurityErrorCode.TOKEN_EXPIRED, e.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidToken(InvalidTokenException e) {
        return problem(SecurityErrorCode.TOKEN_INVALID, e.getMessage());
    }

    @ExceptionHandler(ExpiredOAuthExchangeCodeException.class)
    public ProblemDetail handleExpiredOAuthExchangeCode(ExpiredOAuthExchangeCodeException e) {
        return problem(SecurityErrorCode.OAUTH_EXCHANGE_CODE_EXPIRED, e.getMessage());
    }

    @ExceptionHandler(InvalidOAuthExchangeCodeException.class)
    public ProblemDetail handleInvalidOAuthExchangeCode(InvalidOAuthExchangeCodeException e) {
        return problem(SecurityErrorCode.OAUTH_EXCHANGE_CODE_INVALID, e.getMessage());
    }

    @ExceptionHandler(DuplicateMemberEmailException.class)
    public ProblemDetail handleDuplicateMemberEmailException(DuplicateMemberEmailException e) {
        return problem(MemberErrorCode.MEMBER_EMAIL_DUPLICATE, e.getMessage());
    }

    @ExceptionHandler(InvalidMemberRoleException.class)
    public ProblemDetail handleInvalidMemberRoleException(InvalidMemberRoleException e) {
        return problem(MemberErrorCode.MEMBER_INVALID_ROLE, e.getMessage());
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ProblemDetail handleAuthenticationCredentialsNotFoundException(AuthenticationCredentialsNotFoundException e) {
        return problem(SecurityErrorCode.AUTHENTICATION_REQUIRED, e.getMessage());
    }

    /** 예상하지 못한 예외 — 원인은 서버 로그에만 남기고 일반 메시지로 응답한다. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외 발생", e);
        return problem(CommonErrorCode.COMMON_INTERNAL_ERROR, "서버 내부 오류가 발생했습니다");
    }

    /** 도메인 예외 핸들러 공통 보강 — ErrorCode 한 상수로 status·code를 지정하고 timestamp를 더한다. */
    private ProblemDetail problem(ErrorCode errorCode, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.status(), detail);
        problem.setProperty("code", errorCode.code());
        problem.setProperty("timestamp", OffsetDateTime.now(clock));
        return problem;
    }

    /** 구체 핸들러가 code를 정하지 않은 프레임워크 예외의 fallback — 실제 status 기준으로 고른다. */
    private String defaultCode(HttpStatusCode status) {
        return status.is5xxServerError()
                ? CommonErrorCode.COMMON_INTERNAL_ERROR.code()
                : CommonErrorCode.COMMON_BAD_REQUEST.code();
    }


}
