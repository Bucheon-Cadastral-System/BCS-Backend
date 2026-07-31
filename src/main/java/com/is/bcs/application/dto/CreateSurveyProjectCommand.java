package com.is.bcs.application.dto;

import java.time.LocalDate;

public record CreateSurveyProjectCommand(
        String name,
        LocalDate startedOn,
        LocalDate endedOn,
        String note
) {
}
