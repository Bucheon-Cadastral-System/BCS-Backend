package com.is.bcs.application.dto;

import com.is.bcs.domain.survey.SurveyProjectType;

/** 대상지 CSV 임포트 요청 — 파일 서식과 조사 계기는 별개 축이라 유형은 호출자가 지정한다. */
public record ImportSurveyCsvCommand(
        SurveyProjectType type,
        String name,
        String note,
        byte[] content
) {
}
