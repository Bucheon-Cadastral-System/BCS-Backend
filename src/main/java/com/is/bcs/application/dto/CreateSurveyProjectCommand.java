package com.is.bcs.application.dto;

import java.time.LocalDate;
import java.util.List;

public record CreateSurveyProjectCommand(
        /** 작성자(인증 주체) — 요청 본문이 아니라 인증에서 온다. 인증 없는 호출이면 null. */
        Long authorId,
        String name,
        LocalDate startedOn,
        LocalDate endedOn,
        String note,
        /** 조사 대상 기준점 id — 프로젝트는 점을 지정해 조사 여부를 적는 단위라 비울 수 없다. */
        List<Long> targetPointIds
) {
}
