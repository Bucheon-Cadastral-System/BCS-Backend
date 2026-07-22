package com.is.bcs.adapter.in.web.survey;

import com.is.bcs.application.dto.CreateSurveyProjectCommand;
import com.is.bcs.domain.survey.SurveyProjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSurveyProjectRequest(
        @NotNull(message = "유형은 필수입니다.") SurveyProjectType type,
        @NotBlank(message = "조사명은 필수입니다.") String name,
        String note
) {

    public CreateSurveyProjectCommand toCommand() {
        return new CreateSurveyProjectCommand(type, name, note);
    }
}
