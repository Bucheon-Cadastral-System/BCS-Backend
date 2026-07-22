package com.is.bcs.domain.controlpoint.exception;

/** 기준점 필수값·형식 위반. */
public class InvalidControlPointException extends RuntimeException {

    public InvalidControlPointException(String message) {
        super(message);
    }
}
