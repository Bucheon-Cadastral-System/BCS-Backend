package com.is.bcs.adapter.in.web.survey;

import com.is.bcs.application.dto.CreateSurveyProjectCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateSurveyProjectRequest(
        @NotBlank(message = "조사명은 필수입니다.") String name,
        @NotNull(message = "조사 시작일은 필수입니다.") LocalDate startedOn,
        LocalDate endedOn,
        String note
) {

    public CreateSurveyProjectCommand toCommand() {
        return new CreateSurveyProjectCommand(name, startedOn, endedOn, note);
    }
}
