package com.is.bcs.domain.controlpointimage.exception;

public class ControlPointImageNotFoundException extends RuntimeException {

    public ControlPointImageNotFoundException(String message) {
        super(message);
    }

    public ControlPointImageNotFoundException(Long projectId, Long pointId) {
        super("기준점 이미지를 찾을 수 없습니다. projectId=" + projectId + ", pointId=" + pointId);
    }

}