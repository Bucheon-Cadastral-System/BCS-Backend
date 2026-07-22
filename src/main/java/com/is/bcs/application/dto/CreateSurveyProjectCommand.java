package com.is.bcs.application.dto;

import com.is.bcs.domain.survey.SurveyProjectType;

public record CreateSurveyProjectCommand(
        SurveyProjectType type,
        String name,
        String note
) {
}
