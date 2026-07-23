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
        // 결과별 개수는 서비스가 0으로 채워 넘기지만, 매핑 경계에서도 getOrDefault로 방어한다
        // (long 언박싱이라 키가 없으면 NPE — 부분 맵이 들어와도 0으로 편다)
        return new SurveyProgressSummary(
                progress.projectName(), progress.totalPoints(),
                progress.surveyedPoints(), progress.notSurveyedPoints(),
                progress.countByResult().getOrDefault(SurveyResult.INTACT, 0L),
                progress.countByResult().getOrDefault(SurveyResult.LOST, 0L),
                progress.countByResult().getOrDefault(SurveyResult.ETC, 0L));
    }
}
