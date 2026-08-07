package com.is.bcs.adapter.in.web.survey;

import com.is.bcs.application.dto.SurveyRecordSummary;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;

import java.time.OffsetDateTime;

public record SurveyRecordResponse(
        Long projectId,
        Long pointId,
        SurveyResult result,
        OffsetDateTime surveyedAt,
        String note,
        /** 조사원 표시명 — 인증 없는 기록·파일 임포트 기록은 null 이다. */
        String surveyorName
) {

    public static SurveyRecordResponse from(SurveyRecordSummary summary) {
        SurveyRecord record = summary.record();
        return new SurveyRecordResponse(
                record.getProjectId(), record.getPointId(),
                record.getResult(), record.getSurveyedAt(), record.getNote(), summary.surveyorName());
    }
}
