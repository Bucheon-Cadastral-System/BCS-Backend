package com.is.bcs.adapter.in.web.exception;

import org.springframework.http.HttpStatus;

/** 기준점(CONTROL_POINT_) 도메인 에러 코드. */
public enum ControlPointErrorCode implements ErrorCode {

    /** 기준점 없음 */
    CONTROL_POINT_NOT_FOUND(HttpStatus.NOT_FOUND),

    /** 기준점 필수값·형식 위반 */
    CONTROL_POINT_INVALID(HttpStatus.BAD_REQUEST),

    /** 관리번호 중복 */
    CONTROL_POINT_DUPLICATE(HttpStatus.CONFLICT),

    /** 조사 프로젝트가 대상·기록으로 참조 중 — 삭제 불가 */
    CONTROL_POINT_IN_USE(HttpStatus.CONFLICT),

    /** 수정 창을 열어 둔 사이 다른 사람이 먼저 고침 — 덮어쓰지 않고 거절 */
    CONTROL_POINT_MODIFIED(HttpStatus.CONFLICT),

     /** 현장 이미지 메타데이터를 찾을 수 없음 */
    CONTROL_POINT_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND),

    /** 현장 이미지 파일명 또는 이미지 형식이 유효하지 않음 */
    CONTROL_POINT_IMAGE_INVALID(HttpStatus.BAD_REQUEST);

    private final HttpStatus status;

    ControlPointErrorCode(HttpStatus status) {
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
