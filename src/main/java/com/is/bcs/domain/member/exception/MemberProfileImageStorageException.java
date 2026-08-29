package com.is.bcs.domain.member.exception;

public class MemberProfileImageStorageException extends RuntimeException {

    public MemberProfileImageStorageException(String message) {
        super(message);
    }

    public MemberProfileImageStorageException(String message, Throwable cause) {
        super(message, cause);
    }

}