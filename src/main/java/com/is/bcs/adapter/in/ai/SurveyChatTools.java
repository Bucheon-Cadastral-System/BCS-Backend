package com.is.bcs.adapter.in.ai;

import com.is.bcs.application.dto.SurveyRecordSummary;
import com.is.bcs.application.port.in.controlpoint.GetControlPointsUseCase;
import com.is.bcs.application.port.in.survey.GetSurveyProjectsUseCase;
import com.is.bcs.application.port.in.survey.GetSurveyRecordsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.is.bcs.config.TimeConfig;
import com.is.bcs.domain.controlpoint.ControlPoint;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 챗봇 조회 도구(인바운드 어댑터) — 모델이 도구 호출로 데이터를 묻는 진입점.
 * 조회 유스케이스에 위임만 하고 모델용 표현 변환은 응답 record의 from이 맡는다.
 * 쓰기 유스케이스는 등록하지 않고, 조회 실패 예외는 ChatToolErrorProcessor가 모델용 안내로 바꾼다.
 */
@Component
@RequiredArgsConstructor
public class SurveyChatTools {

    private final GetSurveyProjectsUseCase getSurveyProjectsUseCase;
    private final GetSurveyRecordsUseCase getSurveyRecordsUseCase;
    private final GetControlPointsUseCase getControlPointsUseCase;

    @Tool(description = """
            프로젝트 전체 목록을 조회한다. 각 항목: id · 이름name · 시작일startedOn · 종료일endedOn · 비고note ·
            대상 수totalPoints · 조사한 수surveyedPoints · 진행률progressPercent(%).
            진행률이 여기 이미 들어 있으므로 목록을 훑는 질문에는 getSurveyProgress를 다시 부르지 않는다.
            진행 중인 프로젝트는 progressPercent가 100 미만인 것이다.
            특정 프로젝트 하나의 갈래별 내역(정상 · 망실 · 조사불가 · 기타 · 미조사)이 필요할 때만 그 id를 getSurveyProgress에 넘긴다.""")
    public List<ProjectSummary> getSurveyProjects() {
        return getSurveyProjectsUseCase.getSummaries().stream().map(ProjectSummary::from).toList();
    }

    @Tool(description = """
            지정한 프로젝트 하나의 진행 현황을 조회한다. projectId는 getSurveyProjects로 먼저 찾아 넣는다.
            반환: 프로젝트 이름(projectName) · 대상 전체(totalPoints) · 정상(intactPoints) · 망실(lostPoints) ·
            조사불가(unavailablePoints) · 기타(etcPoints) · 미조사(notSurveyedPoints) · 조사한 수(surveyedPoints) ·
            진행률(progressPercent, %). 모두 계산되어 있으므로 그대로 쓴다.
            projectName은 차트·표 제목과 화면 안내 버튼 글자에 줄이지 말고 전부 옮겨 적는다.
            현황을 나누는 갈래는 정상 · 망실 · 조사불가 · 기타 · 미조사 다섯뿐이다.""")
    public SurveyProgressSummary getSurveyProgress(
            @ToolParam(description = "프로젝트 id(정수) — getSurveyProjects 결과의 id 사용") Long projectId) {
        return SurveyProgressSummary.from(getSurveyRecordsUseCase.getProgress(projectId));
    }

    @Tool(description = """
            프로젝트의 조사 기록을 줄 단위로 조회한다. 어느 점을 무슨 결과로 언제 조사했는지 알 수 있다.
            result를 주면 그 갈래만 남긴다(정상 · 망실 · 조사불가 · 기타). 미조사는 기록이 없는 상태라 여기에 나오지 않는다.
            반환: 관리번호pointNo · 이름name · 결과result · 조사일surveyedOn · 조사원surveyorName · 비고note.
            조사일이 늦은 것부터 limit 만큼만 준다(기본 10, 최대 30). 갈래별 개수만 필요하면 getSurveyProgress를 쓴다.""")
    public List<SurveyRecordBrief> getSurveyRecords(
            @ToolParam(description = "프로젝트 id(정수) — getSurveyProjects 결과의 id 사용") Long projectId,
            @ToolParam(required = false, description = "결과로 거르기 — 정상 · 망실 · 조사불가 · 기타") String result,
            @ToolParam(required = false, description = "최대 건수(기본 10, 최대 30)") Integer limit
    ) {
        String wanted = result == null ? null : result.trim();
        Map<Long, ControlPoint> points = getControlPointsUseCase.getAll().stream()
                .collect(Collectors.toMap(ControlPoint::getId, point -> point));
        return getSurveyRecordsUseCase.getByProjectId(projectId).stream()
                .filter(summary -> wanted == null || wanted.isEmpty()
                        || summary.record().getResult().getDisplayName().equals(wanted))
                .sorted(Comparator.comparing((SurveyRecordSummary summary) -> summary.record().getSurveyedAt()).reversed())
                .limit(bounded(limit))
                .map(summary -> SurveyRecordBrief.of(summary, points.get(summary.record().getPointId()), TimeConfig.KST))
                .toList();
    }

    /** 대화 창에 그릴 수 있는 만큼으로 자른다 — 수백 줄을 넘겨 봐야 모델이 답을 잘라 먹는다. */
    private static int bounded(Integer limit) {
        if (limit == null || limit <= 0) return 10;
        return Math.min(limit, 30);
    }
}
