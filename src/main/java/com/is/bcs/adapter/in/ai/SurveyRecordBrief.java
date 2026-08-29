package com.is.bcs.adapter.in.ai;

import com.is.bcs.application.dto.SurveyRecordSummary;
import com.is.bcs.domain.controlpoint.ControlPoint;

import java.time.LocalDate;
import java.time.ZoneId;

/** 프로젝트 조사 기록 한 줄(모델용) — 어느 점을 무슨 결과로 언제 조사했는지. */
public record SurveyRecordBrief(
        String pointNo,
        String name,
        String result,
        LocalDate surveyedOn,
        String surveyorName,
        String note
) {

    /** 기준점을 못 찾으면 이름 자리를 비운다 — 기록은 남고 점만 지워진 상태에서도 줄이 사라지지 않게 한다. */
    public static SurveyRecordBrief of(SurveyRecordSummary summary, ControlPoint point, ZoneId zone) {
        return new SurveyRecordBrief(
                point == null ? null : point.getPointNo(),
                point == null ? null : point.getName(),
                summary.record().getResult().getDisplayName(),
                summary.record().getSurveyedAt().atZoneSameInstant(zone).toLocalDate(),
                summary.surveyorName(),
                summary.record().getNote());
    }
}
