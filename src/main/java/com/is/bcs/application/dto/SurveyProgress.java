package com.is.bcs.application.dto;

import com.is.bcs.domain.survey.SurveyResult;

import java.util.Map;

/**
 * 조사 프로젝트 진행 현황 — 조사됨=기록 존재(망실 포함), 미조사=전체 기준점−조사됨.
 * countByResult는 모든 결과를 키로 갖는다(없는 결과는 0).
 */
public record SurveyProgress(
        String projectName,
        long totalPoints,
        long surveyedPoints,
        long notSurveyedPoints,
        Map<SurveyResult, Long> countByResult
) {
}
