package com.is.bcs.adapter.in.web.survey;

import com.is.bcs.application.dto.SurveyProjectSummary;

import java.time.LocalDate;

/** 목록 한 행 — 완료 표시(대상·조사 수)와 작성자 표기가 행마다 실려 온다. 완료 여부는 화면이 두 수로 정한다. */
public record SurveyProjectSummaryResponse(
        Long id,
        String name,
        LocalDate startedOn,
        LocalDate endedOn,
        String note,
        long targetCount,
        long surveyedCount,
        /** 작성자 회원 id — 화면이 그 사람의 신원을 물을 때 쓴다. */
        Long authorId,
        /** 작성자 표시명 — 인증이 붙기 전에는 기록이 없어 null 이다. */
        String authorName
) {

    public static SurveyProjectSummaryResponse from(SurveyProjectSummary summary) {
        return new SurveyProjectSummaryResponse(
                summary.project().getId(), summary.project().getName(),
                summary.project().getStartedOn(), summary.project().getEndedOn(), summary.project().getNote(),
                summary.targetCount(), summary.surveyedCount(),
                summary.project().getAuthorId(), summary.authorName());
    }
}
