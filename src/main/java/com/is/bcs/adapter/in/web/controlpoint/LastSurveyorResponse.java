package com.is.bcs.adapter.in.web.controlpoint;

/** 기준점을 마지막으로 조사한 사람의 표시명. 기록이 없거나 인증 없이 남긴 기록이면 null. */
public record LastSurveyorResponse(String name) {
}
