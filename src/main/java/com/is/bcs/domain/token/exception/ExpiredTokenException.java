package com.is.bcs.domain.token.exception;

public class ExpiredTokenException extends InvalidTokenException {

    public ExpiredTokenException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}