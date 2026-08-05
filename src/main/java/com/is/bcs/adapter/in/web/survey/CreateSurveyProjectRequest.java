package com.is.bcs.adapter.in.web.survey;

import com.is.bcs.application.dto.CreateSurveyProjectCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateSurveyProjectRequest(
        @NotBlank(message = "조사명은 필수입니다.") String name,
        @NotNull(message = "조사 시작일은 필수입니다.") LocalDate startedOn,
        LocalDate endedOn,
        String note,
        // 요소 검증까지 건다 — [null] 이 통과하면 조회 단계에서 입력 오류가 서버 오류(5xx)로 둔갑한다
        @NotEmpty(message = "대상 기준점을 1점 이상 지정해 주세요.")
        List<@NotNull(message = "대상 기준점 id는 비울 수 없습니다.") Long> targetPointIds
) {

    public CreateSurveyProjectCommand toCommand() {
        return new CreateSurveyProjectCommand(name, startedOn, endedOn, note, targetPointIds);
    }
}
