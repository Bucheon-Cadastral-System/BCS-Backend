package com.is.bcs.adapter.in.ai;

import com.is.bcs.application.dto.SurveyProgress;
import com.is.bcs.application.dto.SurveyRecordSummary;
import com.is.bcs.application.port.in.survey.GetSurveyProjectsUseCase;
import com.is.bcs.application.port.in.survey.GetSurveyRecordsUseCase;
import com.is.bcs.domain.survey.SurveyProject;
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

    private static final LocalDate ENDED = LocalDate.of(2026, 7, 31);

    private final FakeSurveys fake = new FakeSurveys();
    private final SurveyChatTools tools = new SurveyChatTools(fake, fake);

    @Test
    @DisplayName("프로젝트 목록 — id·이름·기간·비고를 돌려준다")
    void getSurveyProjects_returnsSummaries() {
        fake.projects.put(1L, SurveyProject.restore(1L, null, "2026 일제조사", STARTED, ENDED, "정기 조사"));

        List<ProjectSummary> projects = tools.getSurveyProjects();

        assertEquals(1, projects.size());
        assertEquals(1L, projects.getFirst().id());
        assertEquals("2026 일제조사", projects.getFirst().name());
        assertEquals(STARTED, projects.getFirst().startedOn());
        assertEquals(ENDED, projects.getFirst().endedOn());
        assertEquals("정기 조사", projects.getFirst().note());
    }

    @Test
    @DisplayName("조사 현황 — 화면과 같은 두 축(상태·결과)으로 펼쳐 준다")
    void getSurveyProgress_flattensCounts() {
        fake.progress = new SurveyProgress("2026 일제조사", 5, 3, 2, false,
                Map.of(SurveyResult.INTACT, 2L, SurveyResult.LOST, 1L, SurveyResult.ETC, 0L));

        SurveyProgressSummary progress = tools.getSurveyProgress(1L);

        assertEquals("2026 일제조사", progress.projectName());
        assertEquals(5, progress.totalPoints());
        assertEquals(3, progress.surveyedPoints());
        assertEquals(2, progress.notSurveyedPoints());
        // 조사완료는 조사한 점에서 망실을 뺀 값 — 모델이 직접 빼면 화면과 어긋나므로 여기서 계산해 넘긴다
        assertEquals(2, progress.completedPoints());
        assertEquals(1, progress.lostPoints());
        assertEquals(60, progress.progressPercent()); // 3/5, 화면과 같은 반올림 규칙
    }

    @Test
    @DisplayName("조사완료는 망실이 아닌 기록을 모두 합한 값이다 — 판정값별로 쪼개지 않는다")
    void getSurveyProgress_completedIncludesNonLostResults() {
        fake.progress = new SurveyProgress("굴착협의", 49, 44, 5, false,
                Map.of(SurveyResult.INTACT, 40L, SurveyResult.LOST, 3L, SurveyResult.ETC, 1L));

        SurveyProgressSummary progress = tools.getSurveyProgress(1L);

        assertEquals(41, progress.completedPoints()); // 망실 아닌 기록 41건이 한 값으로 묶인다
        assertEquals(3, progress.lostPoints());
        assertEquals(90, progress.progressPercent()); // 44/49
        // 판정값(완전·조사불가·기타)은 화면에 없는 어휘라 모델에 넘길 필드 자체가 없다
        assertEquals(7, SurveyProgressSummary.class.getRecordComponents().length);
    }

    @Test
    @DisplayName("망실 기록이 없는 현황도 0으로 펼친다 — 매핑 경계 방어(NPE 차단)")
    void getSurveyProgress_missingResultKeys_defaultZero() {
        fake.progress = new SurveyProgress("2026 일제조사", 3, 1, 2, false, Map.of(SurveyResult.INTACT, 1L));

        SurveyProgressSummary progress = tools.getSurveyProgress(1L);

        assertEquals(1L, fake.progressProjectId); // 전달받은 id를 그대로 유스케이스에 넘긴다
        assertEquals(0, progress.lostPoints());
        assertEquals(1, progress.completedPoints());
    }

    @Test
    @DisplayName("대상이 없으면 진행률은 0이다 — 0으로 나누지 않는다")
    void getSurveyProgress_noTarget_zeroPercent() {
        fake.progress = new SurveyProgress("빈 조사", 0, 0, 0, false, Map.of());

        assertEquals(0, tools.getSurveyProgress(1L).progressPercent());
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
        public List<com.is.bcs.application.dto.SurveyProjectSummary> getSummaries() {
            throw new UnsupportedOperationException("챗봇 도구는 목록 요약을 쓰지 않는다");
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
        public List<SurveyRecordSummary> getByProjectId(Long projectId) {
            return List.of();
        }

        @Override
        public List<Long> getTargetPointIds(Long projectId) {
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
