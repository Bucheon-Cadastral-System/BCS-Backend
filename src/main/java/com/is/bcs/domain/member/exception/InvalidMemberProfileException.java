package com.is.bcs.domain.member.exception;

public class InvalidMemberProfileException extends RuntimeException {

    public InvalidMemberProfileException(String message) {
        super(message);
    }
}