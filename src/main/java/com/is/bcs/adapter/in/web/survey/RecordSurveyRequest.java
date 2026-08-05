package com.is.bcs.adapter.in.web.survey;

import com.is.bcs.application.dto.RecordSurveyCommand;
import com.is.bcs.domain.survey.SurveyResult;
import jakarta.validation.constraints.NotNull;

public record RecordSurveyRequest(
        @NotNull(message = "조사 결과는 필수입니다.") SurveyResult result,
        String note
) {

    /** 조사원은 요청 본문이 아니라 인증에서 받는다 — 클라이언트가 지정하면 위조할 수 있다. */
    public RecordSurveyCommand toCommand(Long projectId, Long pointId, Long surveyorId) {
        return new RecordSurveyCommand(projectId, pointId, result, note, surveyorId);
    }
}
