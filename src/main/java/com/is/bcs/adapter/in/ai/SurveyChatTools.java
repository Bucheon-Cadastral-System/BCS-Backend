package com.is.bcs.adapter.in.ai;

import com.is.bcs.application.port.in.survey.GetSurveyProjectsUseCase;
import com.is.bcs.application.port.in.survey.GetSurveyRecordsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 조사 챗봇 조회 도구(인바운드 어댑터) — 모델이 도구 호출로 데이터를 묻는 진입점.
 * 조회 유스케이스에 위임만 하고 모델용 표현 변환은 응답 record의 from이 맡는다.
 * 쓰기 유스케이스는 등록하지 않고, 조회 실패 예외는 ChatToolErrorProcessor가 모델용 안내로 바꾼다.
 */
@Component
@RequiredArgsConstructor
public class SurveyChatTools {

    private final GetSurveyProjectsUseCase getSurveyProjectsUseCase;
    private final GetSurveyRecordsUseCase getSurveyRecordsUseCase;

    @Tool(description = "조사 프로젝트 전체 목록을 조회한다(각 항목: id·이름name·시작일startedOn·종료일endedOn·비고note). 종료일이 비어 있으면 진행 중인 조사다. 사용자가 특정 프로젝트나 조사 현황을 물으면, 먼저 이 도구로 목록을 받아 이름으로 해당 프로젝트를 찾고 그 id를 getSurveyProgress에 넘긴다")
    public List<ProjectSummary> getSurveyProjects() {
        return getSurveyProjectsUseCase.getAll().stream().map(ProjectSummary::from).toList();
    }

    @Tool(description = """
            지정한 조사 프로젝트 하나의 진행 현황을 조회한다. projectId는 getSurveyProjects로 먼저 찾아 넣는다.
            반환: 대상 전체(totalPoints) · 정상(intactPoints) · 망실(lostPoints) · 조사불가(unavailablePoints) ·
            기타(etcPoints) · 미조사(notSurveyedPoints) · 조사한 수(surveyedPoints) · 진행률(progressPercent, %).
            모두 계산되어 있으므로 그대로 쓴다. 조사 현황을 나누는 갈래는 정상 · 망실 · 조사불가 · 기타 · 미조사 다섯뿐이다.""")
    public SurveyProgressSummary getSurveyProgress(
            @ToolParam(description = "조사 프로젝트 id(정수) — getSurveyProjects 결과의 id 사용") Long projectId) {
        return SurveyProgressSummary.from(getSurveyRecordsUseCase.getProgress(projectId));
    }
}
