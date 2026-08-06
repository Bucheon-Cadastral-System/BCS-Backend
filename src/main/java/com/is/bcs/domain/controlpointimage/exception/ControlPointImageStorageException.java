package com.is.bcs.domain.controlpointimage.exception;

/**
 * 이미지 디렉터리 생성, 파일 저장·이동·삭제 또는
 * 외부 WebP 검사 도구 실행에 실패했을 때 발생한다.
 */
public class ControlPointImageStorageException extends RuntimeException {

    public ControlPointImageStorageException(String message) {
        super(message);
    }

    public ControlPointImageStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}