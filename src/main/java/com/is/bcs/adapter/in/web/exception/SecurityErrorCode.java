package com.is.bcs.adapter.in.web.exception;

import org.springframework.http.HttpStatus;

public enum SecurityErrorCode implements ErrorCode {

    OAUTH2_USER_INFO_INVALID(HttpStatus.BAD_GATEWAY),

    OAUTH2_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED),

    OAUTH2_PROVIDER_UNSUPPORTED(HttpStatus.BAD_REQUEST),

    OAUTH2_PRINCIPAL_INVALID(HttpStatus.INTERNAL_SERVER_ERROR),

    TOKEN_INVALID(HttpStatus.UNAUTHORIZED),

    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED);

    private final HttpStatus status;

    SecurityErrorCode(HttpStatus status) {
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