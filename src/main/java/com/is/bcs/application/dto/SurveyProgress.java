package com.is.bcs.application.dto;

import com.is.bcs.domain.survey.SurveyResult;

import java.util.Map;

/**
 * 조사 프로젝트 진행 현황 — 조사됨=기록 존재(망실 포함), 전체=프로젝트 대상 점 수, 미조사=대상−조사됨.
 * complete=대상이 있고 미조사가 0. countByResult는 모든 결과를 키로 갖는다(없는 결과는 0).
 */
public record SurveyProgress(
        String projectName,
        long totalPoints,
        long surveyedPoints,
        long notSurveyedPoints,
        boolean complete,
        Map<SurveyResult, Long> countByResult
) {
}
