package com.is.bcs.application.dto;

import com.is.bcs.domain.survey.SurveyProjectType;

/** 대상지 CSV 임포트 요청 — 유형은 조사의 계기(일반·굴착협의)이고, 파일 서식과는 별개 축이라 호출자가 지정한다. */
public record ImportSurveyCsvCommand(
        SurveyProjectType type,
        String name,
        String note,
        byte[] content
) {
}
