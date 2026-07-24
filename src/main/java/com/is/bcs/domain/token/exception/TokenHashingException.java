package com.is.bcs.domain.token.exception;

public class TokenHashingException extends RuntimeException {

    public TokenHashingException(String message) {
        super(message);
    }

    public TokenHashingException(String message, Throwable cause) {
        super(message, cause);
    }
}