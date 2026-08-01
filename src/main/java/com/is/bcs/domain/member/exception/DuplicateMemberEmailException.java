package com.is.bcs.domain.member.exception;

public class DuplicateMemberEmailException extends RuntimeException {

    public DuplicateMemberEmailException(String message) {
        super(message);
    }
}