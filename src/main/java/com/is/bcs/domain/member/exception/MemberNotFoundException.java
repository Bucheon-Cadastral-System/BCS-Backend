package com.is.bcs.domain.member.exception;

/** 회원 조회 실패. */
public class MemberNotFoundException extends RuntimeException {

    public MemberNotFoundException(String message) {
        super(message);
    }
}
