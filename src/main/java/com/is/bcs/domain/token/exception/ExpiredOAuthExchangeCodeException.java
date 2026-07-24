package com.is.bcs.domain.token.exception;

public class ExpiredOAuthExchangeCodeException extends InvalidOAuthExchangeCodeException {

    public ExpiredOAuthExchangeCodeException(String message) {
        super(message);
    }

}