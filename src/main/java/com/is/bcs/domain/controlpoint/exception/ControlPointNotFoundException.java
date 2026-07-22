package com.is.bcs.domain.controlpoint.exception;

/** 기준점 조회 실패. */
public class ControlPointNotFoundException extends RuntimeException {

    public ControlPointNotFoundException(String message) {
        super(message);
    }
}
