package com.is.bcs.domain.controlpoint.exception;

/** 조사 프로젝트가 대상·기록으로 참조하는 기준점 — 조사 데이터는 프로젝트 소유라 점 삭제가 지울 수 없다. */
public class ControlPointInUseException extends RuntimeException {

    public ControlPointInUseException(String message) {
        super(message);
    }
}
