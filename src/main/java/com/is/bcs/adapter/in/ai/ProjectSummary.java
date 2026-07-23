package com.is.bcs.adapter.in.ai;

import com.is.bcs.domain.survey.SurveyProject;

/** 조사 프로젝트 요약(모델용) — 유형은 한글 표시명으로 푼다. */
public record ProjectSummary(Long id, String name, String type, String note) {

    public static ProjectSummary from(SurveyProject project) {
        return new ProjectSummary(
                project.getId(), project.getName(), project.getType().getDisplayName(), project.getNote());
    }
}
