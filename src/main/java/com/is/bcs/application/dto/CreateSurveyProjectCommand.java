package com.is.bcs.application.dto;

import java.time.LocalDate;
import java.util.List;

public record CreateSurveyProjectCommand(
        String name,
        LocalDate startedOn,
        LocalDate endedOn,
        String note,
        /** 조사 대상 기준점 id — 프로젝트는 점을 지정해 조사 여부를 적는 단위라 비울 수 없다. */
        List<Long> targetPointIds
) {
}
