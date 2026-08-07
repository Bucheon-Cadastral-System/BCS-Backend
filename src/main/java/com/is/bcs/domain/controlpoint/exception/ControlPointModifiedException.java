package com.is.bcs.domain.controlpoint.exception;

/**
 * 수정 창을 열어 둔 사이 다른 사람이 그 기준점을 먼저 고쳤다.
 *
 * <p>덮어쓰지 않고 거절한다. 성과 좌표처럼 되돌리기 어려운 값이 담겨 있어, 나중에 저장한 쪽이 이기면
 * 앞 사람의 수정이 흔적 없이 사라진다.
 */
public class ControlPointModifiedException extends RuntimeException {

    public ControlPointModifiedException(String message) {
        super(message);
    }
}
