package com.is.bcs.adapter.in.web.survey;

import com.is.bcs.application.dto.SurveyProgress;
import com.is.bcs.domain.survey.SurveyResult;

import java.util.Map;

/** 조사 진행 현황 응답 — 전체=프로젝트 대상 점 수, 조사됨=기록(망실 포함), complete=대상이 있고 미조사 0. */
public record SurveyProgressResponse(
        String projectName,
        long totalPoints,
        long surveyedPoints,
        long notSurveyedPoints,
        boolean complete,
        Map<SurveyResult, Long> countByResult
) {

    public static SurveyProgressResponse from(SurveyProgress progress) {
        return new SurveyProgressResponse(
                progress.projectName(), progress.totalPoints(),
                progress.surveyedPoints(), progress.notSurveyedPoints(),
                progress.complete(), progress.countByResult());
    }
}
