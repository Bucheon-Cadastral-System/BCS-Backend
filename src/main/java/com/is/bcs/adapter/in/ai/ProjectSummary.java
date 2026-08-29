package com.is.bcs.adapter.in.ai;

import com.is.bcs.application.dto.SurveyProjectSummary;

import java.time.LocalDate;

/**
 * 프로젝트 요약(모델용).
 *
 * <p>대상 수와 진행률을 함께 싣는다. 목록만 주면 모델이 프로젝트마다 현황 도구를 다시 불러야 하는데,
 * 등록된 프로젝트가 백 건을 넘으면 그 호출을 다 돌 수 없어 진행률을 지어내게 된다.
 */
public record ProjectSummary(
        Long id,
        String name,
        LocalDate startedOn,
        LocalDate endedOn,
        String note,
        long totalPoints,
        long surveyedPoints,
        int progressPercent
) {

    public static ProjectSummary from(SurveyProjectSummary summary) {
        return new ProjectSummary(
                summary.project().getId(), summary.project().getName(),
                summary.project().getStartedOn(), summary.project().getEndedOn(), summary.project().getNote(),
                summary.targetCount(), summary.surveyedCount(),
                SurveyProgressSummary.percent(summary.surveyedCount(), summary.targetCount()));
    }
}
