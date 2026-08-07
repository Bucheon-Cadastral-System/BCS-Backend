package com.is.bcs.adapter.in.web.survey;

import com.is.bcs.application.dto.SurveyRecordSummary;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;

import java.time.OffsetDateTime;

/**
 * 조사기록 한 줄. 프로젝트 id 는 싣지 않는다 — 이 목록은 이미 한 프로젝트로 좁혀 조회한 결과라
 * 행마다 같은 값을 다시 나르게 된다.
 */
public record SurveyRecordResponse(
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
                record.getPointId(), record.getResult(), record.getSurveyedAt(),
                record.getNote(), summary.surveyorName());
    }
}
