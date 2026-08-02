package com.is.bcs.adapter.in.web.exception;

import org.springframework.http.HttpStatus;

/** 회원(MEMBER_) 도메인 에러 코드. */
public enum MemberErrorCode implements ErrorCode {

    /** 회원 없음 */
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND),

    /** 상태 전이 규칙 위반(승인·비활성화·재활성화 등) */
    MEMBER_INVALID_STATE(HttpStatus.UNPROCESSABLE_CONTENT),

    /** 프로필 필수값 위반 */
    MEMBER_PROFILE_INVALID(HttpStatus.BAD_REQUEST),

    /** 이메일 중복 */
    MEMBER_EMAIL_DUPLICATE(HttpStatus.CONFLICT),

    /** 회원 권한 전이 규칙 위반 */
    MEMBER_INVALID_ROLE(HttpStatus.UNPROCESSABLE_CONTENT);

    private final HttpStatus status;

    MemberErrorCode(HttpStatus status) {
        this.status = status;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus status() {
        return status;
    }
}
