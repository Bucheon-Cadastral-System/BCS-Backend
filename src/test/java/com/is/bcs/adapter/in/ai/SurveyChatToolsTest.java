package com.is.bcs.adapter.in.ai;

import com.is.bcs.application.dto.SurveyProgress;
import com.is.bcs.application.port.in.survey.GetSurveyProjectsUseCase;
import com.is.bcs.application.port.in.survey.GetSurveyRecordsUseCase;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import com.is.bcs.domain.survey.exception.SurveyProjectNotFoundException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 조사 챗봇 도구 검증 — 유스케이스 결과를 모델용 표현으로 매핑하는 것만 본다. */
class SurveyChatToolsTest {

    private static final LocalDate STARTED = LocalDate.of(2026, 7, 1);

    private final FakeSurveys fake = new FakeSurveys();
    private final SurveyChatTools tools = new SurveyChatTools(fake, fake);

    @Test
    @DisplayName("프로젝트 목록 — id·이름·기간·비고를 돌려준다")
    void getSurveyProjects_returnsSummaries() {
        fake.projects.put(1L, SurveyProject.restore(1L, null, "2026 일제조사", STARTED, null, "정기 조사"));

        List<ProjectSummary> projects = tools.getSurveyProjects();

        assertEquals(1, projects.size());
        assertEquals(1L, projects.getFirst().id());
        assertEquals("2026 일제조사", projects.getFirst().name());
        assertEquals(STARTED, projects.getFirst().startedOn());
        assertEquals("정기 조사", projects.getFirst().note());
    }

    @Test
    @DisplayName("조사 현황 — 유스케이스 진행 현황을 결과별 개수 필드로 펼쳐 준다")
    void getSurveyProgress_flattensCounts() {
        fake.progress = new SurveyProgress("2026 일제조사", 5, 3, 2, false,
                Map.of(SurveyResult.INTACT, 2L, SurveyResult.LOST, 1L, SurveyResult.ETC, 0L));

        SurveyProgressSummary progress = tools.getSurveyProgress(1L);

        assertEquals("2026 일제조사", progress.projectName());
        assertEquals(5, progress.totalPoints());
        assertEquals(3, progress.surveyedPoints());
        assertEquals(2, progress.notSurveyedPoints());
        assertEquals(2, progress.intactPoints());
        assertEquals(1, progress.lostPoints());
        assertEquals(0, progress.etcPoints());
    }

    @Test
    @DisplayName("결과 유형이 빠진 현황도 0으로 펼친다 — 매핑 경계 방어(NPE 차단)")
    void getSurveyProgress_missingResultKeys_defaultZero() {
        fake.progress = new SurveyProgress("2026 일제조사", 3, 1, 2, false, Map.of(SurveyResult.INTACT, 1L));

        SurveyProgressSummary progress = tools.getSurveyProgress(1L);

        assertEquals(1L, fake.progressProjectId); // 전달받은 id를 그대로 유스케이스에 넘긴다
        assertEquals(1, progress.intactPoints());
        assertEquals(0, progress.lostPoints());
        assertEquals(0, progress.etcPoints());
    }

    @Test
    @DisplayName("조사 현황 실패 예외는 잡지 않고 그대로 올린다 — 정형화는 ChatToolErrorProcessor 몫")
    void getSurveyProgress_missing_propagates() {
        assertThrows(SurveyProjectNotFoundException.class, () -> tools.getSurveyProgress(99L));
        assertEquals(99L, fake.progressProjectId); // 실패 경로에서도 전달받은 id를 그대로 넘긴다
    }

    /** 조사 조회 유스케이스 페이크. */
    private static class FakeSurveys implements GetSurveyProjectsUseCase, GetSurveyRecordsUseCase {

        final Map<Long, SurveyProject> projects = new HashMap<>();
        SurveyProgress progress;
        Long progressProjectId; // getProgress에 전달된 id 기록 — 도구가 올바른 id를 넘기는지 검증용

        @Override
        public List<SurveyProject> getAll() {
            return List.copyOf(projects.values());
        }

        @Override
        public SurveyProject getById(Long id) {
            SurveyProject project = projects.get(id);
            if (project == null) {
                throw new SurveyProjectNotFoundException("조사 프로젝트를 찾을 수 없습니다: " + id);
            }
            return project;
        }

        @Override
        public List<SurveyRecord> getByProjectId(Long projectId) {
            return List.of();
        }

        @Override
        public SurveyProgress getProgress(Long projectId) {
            this.progressProjectId = projectId;
            if (progress == null) {
                throw new SurveyProjectNotFoundException("조사 프로젝트를 찾을 수 없습니다: " + projectId);
            }
            return progress;
        }
    }
}
