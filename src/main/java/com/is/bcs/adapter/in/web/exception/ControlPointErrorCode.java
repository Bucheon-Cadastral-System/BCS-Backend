package com.is.bcs.adapter.in.web.exception;

import org.springframework.http.HttpStatus;

/** 기준점(CONTROL_POINT_) 도메인 에러 코드. */
public enum ControlPointErrorCode implements ErrorCode {

    /** 기준점 없음 */
    CONTROL_POINT_NOT_FOUND(HttpStatus.NOT_FOUND),

    /** 기준점 필수값·형식 위반 */
    CONTROL_POINT_INVALID(HttpStatus.BAD_REQUEST),

    /** 관리번호 중복 */
    CONTROL_POINT_DUPLICATE(HttpStatus.CONFLICT);

    private final HttpStatus status;

    ControlPointErrorCode(HttpStatus status) {
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
