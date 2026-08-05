package com.is.bcs.application.port.in.controlpoint;

public interface DeleteControlPointUseCase {

    void delete(Long pointId);

    /** 조사 데이터(대상·기록)가 이 점을 참조하는지 — 화면이 삭제 확인 창을 열기 전에 가부를 갈라 보여 주는 데 쓴다. */
    boolean isReferenced(Long pointId);
}
