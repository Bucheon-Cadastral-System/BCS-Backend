package com.is.bcs.adapter.in.ai;

import com.is.bcs.application.dto.LastSurveySummary;
import com.is.bcs.domain.controlpoint.ControlPoint;

import java.time.LocalDate;

/**
 * 기준점 하나의 최종조사(모델용) — 회차와 무관하게 가장 마지막 조사다.
 *
 * <p>조사한 적이 없으면 결과와 조사일이 비어 있다. 조사원은 파일로 들어온 조사와 인증 전 기록에 없다.
 */
public record LastSurveyBrief(
        String pointNo,
        String name,
        String result,
        LocalDate surveyedOn,
        String surveyorName,
        String note
) {

    public static LastSurveyBrief from(ControlPoint point, LastSurveySummary summary) {
        return new LastSurveyBrief(
                point.getPointNo(), point.getName(),
                summary.result(), summary.surveyedOn(), summary.surveyorName(), summary.note());
    }
}
