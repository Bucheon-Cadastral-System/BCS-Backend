package com.is.bcs.adapter.in.web.survey;

import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyProjectType;

public record SurveyProjectResponse(
        Long id,
        SurveyProjectType type,
        String name,
        String note
) {

    public static SurveyProjectResponse from(SurveyProject project) {
        return new SurveyProjectResponse(
                project.getId(), project.getType(), project.getName(), project.getNote());
    }
}
