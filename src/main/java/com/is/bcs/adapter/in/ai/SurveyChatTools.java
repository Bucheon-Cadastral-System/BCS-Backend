package com.is.bcs.adapter.in.ai;

import com.is.bcs.application.dto.SurveyProgress;
import com.is.bcs.application.port.in.survey.GetSurveyProjectsUseCase;
import com.is.bcs.application.port.in.survey.GetSurveyRecordsUseCase;
import com.is.bcs.domain.survey.SurveyResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 조사 챗봇 조회 도구(인바운드 어댑터) — 모델이 도구 호출로 데이터를 묻는 진입점.
 * 조회 유스케이스에 위임하고 표시용 한글 값으로 매핑만 한다. 쓰기 유스케이스는 등록하지 않는다.
 * 조회 실패 예외는 여기서 잡지 않는다 — ChatToolErrorProcessor가 모델용 안내로 바꾼다.
 */
@Component
@RequiredArgsConstructor
public class SurveyChatTools {

    private final GetSurveyProjectsUseCase getSurveyProjectsUseCase;
    private final GetSurveyRecordsUseCase getSurveyRecordsUseCase;

    public record ProjectSummary(Long id, String name, String type, String note) {
    }

    public record SurveyProgressSummary(
            String projectName, long totalPoints,
            long surveyedPoints, long notSurveyedPoints,
            long intactPoints, long lostPoints, long etcPoints
    ) {
    }

    @Tool(description = "조사 프로젝트 목록(id·이름·유형·비고)을 조회한다. 조사 현황을 물으면 먼저 이걸로 프로젝트 id를 찾는다.")
    public List<ProjectSummary> getSurveyProjects() {
        return getSurveyProjectsUseCase.getAll().stream()
                .map(p -> new ProjectSummary(p.getId(), p.getName(), p.getType().getDisplayName(), p.getNote()))
                .toList();
    }

    @Tool(description = "조사 프로젝트의 진행 현황(전체 기준점 수, 조사됨·미조사 수, 완전·망실·기타 수)을 조회한다.")
    public SurveyProgressSummary getSurveyProgress(
            @ToolParam(description = "조사 프로젝트 id") Long projectId) {
        SurveyProgress progress = getSurveyRecordsUseCase.getProgress(projectId);
        return new SurveyProgressSummary(
                progress.projectName(), progress.totalPoints(),
                progress.surveyedPoints(), progress.notSurveyedPoints(),
                progress.countByResult().get(SurveyResult.INTACT),
                progress.countByResult().get(SurveyResult.LOST),
                progress.countByResult().get(SurveyResult.ETC));
    }
}
