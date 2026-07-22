package com.is.bcs.adapter.in.web.survey;

import com.is.bcs.application.dto.RecordSurveyCommand;
import com.is.bcs.domain.survey.SurveyResult;
import jakarta.validation.constraints.NotNull;

public record RecordSurveyRequest(
        @NotNull(message = "조사 결과는 필수입니다.") SurveyResult result,
        String note
) {

    public RecordSurveyCommand toCommand(Long projectId, Long pointId) {
        return new RecordSurveyCommand(projectId, pointId, result, note);
    }
}
