package com.is.bcs.adapter.in.web.exception;

public class InvalidCursorException extends RuntimeException {

    public InvalidCursorException() {
        super("유효하지 않은 커서입니다.");
    }
}