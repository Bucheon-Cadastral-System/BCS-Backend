package com.is.bcs.domain.member.exception;

/** 회원 프로필 필수값 위반 — 웹 계층 검증을 지나쳐 도메인까지 온 잘못된 입력. */
public class InvalidMemberProfileException extends RuntimeException {

    public InvalidMemberProfileException(String message) {
        super(message);
    }
}
