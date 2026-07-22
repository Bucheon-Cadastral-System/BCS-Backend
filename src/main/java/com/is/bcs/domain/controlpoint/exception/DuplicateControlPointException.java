package com.is.bcs.domain.controlpoint.exception;

/** 관리번호 중복 — 이미 등록된 기준점. */
public class DuplicateControlPointException extends RuntimeException {

    public DuplicateControlPointException(String message) {
        super(message);
    }
}
