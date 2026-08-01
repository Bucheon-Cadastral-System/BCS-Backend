package com.is.bcs.domain.member.exception;

/**
 * 회원 권한 전이 규칙 위반.
 */
public class InvalidMemberRoleException extends RuntimeException {

    public InvalidMemberRoleException(String message) {
        super(message);
    }
}