package com.is.bcs.adapter.in.web.exception;

import org.springframework.http.HttpStatus;

/**
 * 공통 에러 코드(COMMON_·AUTH_). 상수명이 곧 응답 code 값이다.
 *
 * fallback 코드(COMMON_BAD_REQUEST·COMMON_INTERNAL_ERROR)의 status는 대표값이며,
 * 실제 응답 status는 프레임워크가 정한 값을 따른다(GlobalExceptionHandler.defaultCode).
 */
public enum CommonErrorCode implements ErrorCode {

    /** 요청 바디·파라미터 검증 실패 */
    COMMON_INVALID_INPUT(HttpStatus.BAD_REQUEST),

    /** 그 외 잘못된 요청(405·415 등 미분류 4xx) — 상태 기반 fallback */
    COMMON_BAD_REQUEST(HttpStatus.BAD_REQUEST),

    /** 미처리 예외·미분류 5xx fallback */
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),

    /** 인증 실패(토큰 없음·만료·위조) — 보안 예외 핸들러에서 사용 */
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED),

    /** 권한 부족 — 보안 예외 핸들러에서 사용 */
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN);

    private final HttpStatus status;

    CommonErrorCode(HttpStatus status) {
        this.status = status;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus status() {
        return status;
    }
}
