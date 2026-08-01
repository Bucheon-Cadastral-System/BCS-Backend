package com.is.bcs.adapter.in.ai;

import com.is.bcs.domain.survey.SurveyProject;

import java.time.LocalDate;

/** 조사 프로젝트 요약(모델용). */
public record ProjectSummary(Long id, String name, LocalDate startedOn, LocalDate endedOn, String note) {

    public static ProjectSummary from(SurveyProject project) {
        return new ProjectSummary(
                project.getId(), project.getName(),
                project.getStartedOn(), project.getEndedOn(), project.getNote());
    }
}
