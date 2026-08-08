package com.is.bcs.application.dto;

import com.is.bcs.domain.survey.SurveyRecord;

/** 조사 기록 + 조사원 표시명 — 화면이 '누가 조사했는지'를 그릴 수 있게 이름을 동봉한다(없으면 null). */
public record SurveyRecordSummary(SurveyRecord record, String surveyorName) {
}
