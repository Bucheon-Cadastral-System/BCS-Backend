package com.is.bcs.adapter.in.ai;

import com.is.bcs.application.dto.SurveyProgress;
import com.is.bcs.domain.survey.SurveyResult;

/** 조사 진행 현황(모델용) — 결과별 개수를 이름 있는 필드로 편다. */
public record SurveyProgressSummary(
        String projectName, long totalPoints,
        long surveyedPoints, long notSurveyedPoints,
        long intactPoints, long lostPoints, long etcPoints
) {

    public static SurveyProgressSummary from(SurveyProgress progress) {
        return new SurveyProgressSummary(
                progress.projectName(), progress.totalPoints(),
                progress.surveyedPoints(), progress.notSurveyedPoints(),
                progress.countByResult().get(SurveyResult.INTACT),
                progress.countByResult().get(SurveyResult.LOST),
                progress.countByResult().get(SurveyResult.ETC));
    }
}
