package com.is.bcs.application.dto;

import java.time.LocalDate;
import java.util.List;

public record UpdateSurveyProjectCommand(
        Long projectId,
        String name,
        LocalDate startedOn,
        LocalDate endedOn,
        String note,
        /** 수정 후의 대상 전체 — 부분 수정이 아니라 통째로 다시 적는 값이다(생성과 같은 규칙, 최소 1점). */
        List<Long> targetPointIds
) {
}
