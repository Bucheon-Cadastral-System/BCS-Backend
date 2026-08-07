package com.is.bcs.adapter.in.web.controlpoint;

import com.is.bcs.application.dto.LastSurveySummary;

import java.time.LocalDate;

/** 기준점의 최종조사 요약. 세 값이 함께 바뀌므로 한 번에 내려보낸다. */
public record LastSurveyResponse(String result, LocalDate surveyedOn, String surveyorName, String note) {

    public static LastSurveyResponse from(LastSurveySummary summary) {
        return new LastSurveyResponse(summary.result(), summary.surveyedOn(), summary.surveyorName(), summary.note());
    }
}
