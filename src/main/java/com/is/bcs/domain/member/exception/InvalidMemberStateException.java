package com.is.bcs.domain.member.exception;

/** 회원 상태 전이 규칙 위반 — 현재 상태에서 허용되지 않는 처리(승인·비활성화·재활성화 등). */
public class InvalidMemberStateException extends RuntimeException {

    public InvalidMemberStateException(String message) {
        super(message);
    }
}
