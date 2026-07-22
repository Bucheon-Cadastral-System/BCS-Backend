package com.is.bcs.application.dto;

import com.is.bcs.domain.survey.SurveyResult;

public record RecordSurveyCommand(
        Long projectId,
        Long pointId,
        SurveyResult result,
        String note
) {
}
