package com.is.bcs.domain.token.exception;

public class InvalidOAuthExchangeCodeException extends RuntimeException {

    public InvalidOAuthExchangeCodeException(String message) {
        super(message);
    }

}