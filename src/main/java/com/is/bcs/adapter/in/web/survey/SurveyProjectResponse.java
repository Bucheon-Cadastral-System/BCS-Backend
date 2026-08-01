package com.is.bcs.adapter.in.web.survey;

import com.is.bcs.domain.survey.SurveyProject;

import java.time.LocalDate;

public record SurveyProjectResponse(
        Long id,
        String name,
        LocalDate startedOn,
        LocalDate endedOn,
        String note
) {

    public static SurveyProjectResponse from(SurveyProject project) {
        return new SurveyProjectResponse(
                project.getId(), project.getName(),
                project.getStartedOn(), project.getEndedOn(), project.getNote());
    }
}
