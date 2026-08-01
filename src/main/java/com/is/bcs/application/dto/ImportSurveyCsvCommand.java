package com.is.bcs.application.dto;

import java.time.LocalDate;

/** 대상지 파일 임포트 요청 — 파일과 함께 만들 조사 프로젝트의 값을 받는다. */
public record ImportSurveyCsvCommand(
        String name,
        LocalDate startedOn,
        LocalDate endedOn,
        String note,
        byte[] content
) {
}
