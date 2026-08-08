package com.is.bcs.application.dto;

import com.is.bcs.domain.survey.SurveyResult;

public record RecordSurveyCommand(
        Long projectId,
        Long pointId,
        SurveyResult result,
        String note,
        /** 조사원(인증 주체) — 요청 본문이 아니라 인증에서 온다. 인증 없는 호출이면 null. */
        Long surveyorId
) {
}
