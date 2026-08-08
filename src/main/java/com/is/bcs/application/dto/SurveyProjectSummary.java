package com.is.bcs.application.dto;

import com.is.bcs.domain.survey.SurveyProject;

/**
 * 목록 화면용 요약 — 행마다 완료 여부·작성자를 그리려면 목록 한 번에 실려 와야 한다(건별 진행률 조회는 N+1).
 * 완료 판정은 파생값이라 내려보내지 않는다: 화면이 targetCount·surveyedCount 로 정한다.
 */
public record SurveyProjectSummary(
        SurveyProject project,
        long targetCount,
        long surveyedCount,
        /** 작성자 표시명 — 인증이 붙기 전에는 기록이 없어 null 이다. */
        String authorName
) {
}
