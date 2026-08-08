package com.is.bcs.adapter.in.web.controlpoint;

/** 조사 데이터(대상·기록)가 이 점을 참조하는지 — 화면이 삭제 확인 창을 열기 전에 가부를 가르는 데 쓴다. */
public record ControlPointUsageResponse(boolean referenced) {
}
