package com.is.bcs.application.dto;

import java.time.LocalDate;

/** 대상지 파일 임포트 요청 — 파일과 함께 만들 조사 프로젝트의 값을 받는다. */
public record ImportSurveyCsvCommand(
        /** 작성자(인증 주체) — 요청 본문이 아니라 인증에서 온다. 인증 없는 호출이면 null. */
        Long authorId,
        String name,
        LocalDate startedOn,
        LocalDate endedOn,
        String note,
        byte[] content
) {
}
