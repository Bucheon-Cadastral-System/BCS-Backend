package com.is.bcs.domain.controlpoint.exception;

/** 기준점 필수값·형식 위반. */
public class InvalidControlPointException extends RuntimeException {

    public InvalidControlPointException(String message) {
        super(message);
    }

    /** 바깥 라이브러리가 알려 온 실패를 감쌀 때 — 사용자 문구는 message 로, 진단은 cause 로 남긴다. */
    public InvalidControlPointException(String message, Throwable cause) {
        super(message, cause);
    }
}
