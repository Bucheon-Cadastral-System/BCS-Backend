package com.is.bcs.adapter.in.web.survey;

import com.is.bcs.application.dto.UpdateSurveyProjectCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** 수정은 이름·기간·비고만 다룬다 — 대상 재지정은 기록과 얽혀 별도 결정이 필요한 사안이라 받지 않는다. */
public record UpdateSurveyProjectRequest(
        @NotBlank(message = "조사명은 필수입니다.") String name,
        @NotNull(message = "조사 시작일은 필수입니다.") LocalDate startedOn,
        LocalDate endedOn,
        String note
) {

    public UpdateSurveyProjectCommand toCommand(Long projectId) {
        return new UpdateSurveyProjectCommand(projectId, name, startedOn, endedOn, note);
    }
}
