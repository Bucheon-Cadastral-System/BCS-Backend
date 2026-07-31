package com.is.bcs.application.dto;

import java.time.LocalDate;
import java.util.Map;

/** 대상지 파일 임포트 요청 — 파일과 함께 만들 조사 프로젝트의 값을 받는다. */
public record ImportSurveyCsvCommand(
        String name,
        LocalDate startedOn,
        LocalDate endedOn,
        String note,
        byte[] content,
        /** 파일의 열 이름 → 읽어 들일 항목. 담당자가 매핑을 고쳤을 때만 채워진다. */
        Map<String, String> columnOverrides
) {
}
