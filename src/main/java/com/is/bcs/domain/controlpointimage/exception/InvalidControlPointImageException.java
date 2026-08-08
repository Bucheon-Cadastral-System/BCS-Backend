package com.is.bcs.domain.controlpointimage.exception;

/** 기준점 현장 사진의 형식·크기·메타데이터가 유효하지 않을 때 발생한다. */
public class InvalidControlPointImageException extends RuntimeException {

    public InvalidControlPointImageException(String message) {

        super(message);

    }
}