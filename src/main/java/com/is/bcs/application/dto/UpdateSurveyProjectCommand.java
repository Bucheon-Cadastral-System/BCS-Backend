package com.is.bcs.application.dto;

import java.time.LocalDate;

public record UpdateSurveyProjectCommand(
        Long projectId,
        String name,
        LocalDate startedOn,
        LocalDate endedOn,
        String note
) {
}
