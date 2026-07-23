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

    @Tool(description = "조사 프로젝트 전체 목록을 조회한다(각 항목: id·이름name·유형type·비고note). 유형은 한글 표시명으로 반환한다(예: 일반·굴착협의). 사용자가 특정 프로젝트나 조사 현황을 물으면, 먼저 이 도구로 목록을 받아 이름·유형으로 해당 프로젝트를 찾고 그 id를 getSurveyProgress에 넘긴다")
    public List<ProjectSummary> getSurveyProjects() {
        return getSurveyProjectsUseCase.getAll().stream().map(ProjectSummary::from).toList();
    }

    @Tool(description = "지정한 조사 프로젝트 하나의 진행 현황을 조회한다. 반환: 프로젝트명(projectName)·전체 기준점 수(totalPoints)·조사됨(surveyedPoints)·미조사(notSurveyedPoints)·결과별 완전(intactPoints)·망실(lostPoints)·기타(etcPoints). projectId는 getSurveyProjects로 먼저 찾아 넣는다")
    public SurveyProgressSummary getSurveyProgress(
            @ToolParam(description = "조사 프로젝트 id(정수) — getSurveyProjects 결과의 id 사용") Long projectId) {
        return SurveyProgressSummary.from(getSurveyRecordsUseCase.getProgress(projectId));
    }
}
