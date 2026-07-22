package com.is.bcs.adapter.in.web.survey;

import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;

import java.time.OffsetDateTime;

public record SurveyRecordResponse(
        Long id,
        Long projectId,
        Long pointId,
        SurveyResult result,
        OffsetDateTime surveyedAt,
        String note
) {

    public static SurveyRecordResponse from(SurveyRecord record) {
        return new SurveyRecordResponse(
                record.getId(), record.getProjectId(), record.getPointId(),
                record.getResult(), record.getSurveyedAt(), record.getNote());
    }
}
