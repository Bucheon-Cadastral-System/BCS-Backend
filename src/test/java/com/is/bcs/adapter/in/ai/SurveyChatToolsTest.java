package com.is.bcs.adapter.in.ai;

import com.is.bcs.application.dto.SurveyProgress;
import com.is.bcs.application.dto.SurveyRecordSummary;
import com.is.bcs.application.port.in.controlpoint.GetControlPointsUseCase;
import com.is.bcs.application.port.in.survey.GetSurveyProjectsUseCase;
import com.is.bcs.application.port.in.survey.GetSurveyRecordsUseCase;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyResult;
import com.is.bcs.domain.survey.exception.SurveyProjectNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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
    private final FakePoints points = new FakePoints();
    private final SurveyChatTools tools = new SurveyChatTools(fake, fake, points);

    @Test
    @DisplayName("프로젝트 목록 — id·이름·기간·비고를 돌려준다")
    void getSurveyProjects_returnsSummaries() {
        fake.projects.put(1L, SurveyProject.restore(1L, null, "2026 일제조사", STARTED, ENDED, "정기 조사"));
        fake.targetCounts.put(1L, 40L);
        fake.surveyedCounts.put(1L, 10L);

        List<ProjectSummary> projects = tools.getSurveyProjects();

        assertEquals(1, projects.size());
        assertEquals(1L, projects.getFirst().id());
        assertEquals("2026 일제조사", projects.getFirst().name());
        assertEquals(STARTED, projects.getFirst().startedOn());
        assertEquals(ENDED, projects.getFirst().endedOn());
        assertEquals("정기 조사", projects.getFirst().note());
        assertEquals(40, projects.getFirst().totalPoints());
        assertEquals(10, projects.getFirst().surveyedPoints());
        assertEquals(25, projects.getFirst().progressPercent());
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
        // 정상은 조사한 점에서 망실을 뺀 값 — 모델이 직접 빼면 화면과 어긋나므로 여기서 계산해 넘긴다
        assertEquals(2, progress.intactPoints());
        assertEquals(1, progress.lostPoints());
        assertEquals(60, progress.progressPercent()); // 3/5, 화면과 같은 반올림 규칙
    }

    @Test
    @DisplayName("정상은 조사한 점에서 망실·조사불가·기타를 뺀 값이다")
    void getSurveyProgress_intactExcludesOtherResults() {
        fake.progress = new SurveyProgress("굴착협의", 50, 45, 5, false,
                Map.of(SurveyResult.INTACT, 40L, SurveyResult.LOST, 3L, SurveyResult.UNAVAILABLE, 1L, SurveyResult.ETC, 1L));

        SurveyProgressSummary progress = tools.getSurveyProgress(1L);

        assertEquals(40, progress.intactPoints()); // 조사불가·기타는 정상에 섞이지 않는다
        assertEquals(3, progress.lostPoints());
        assertEquals(1, progress.unavailablePoints());
        assertEquals(1, progress.etcPoints());
        assertEquals(90, progress.progressPercent()); // 45/50
        // 화면이 쓰는 다섯 갈래(정상·망실·조사불가·기타·미조사)를 모두 필드로 두므로 아홉 개다
        assertEquals(9, SurveyProgressSummary.class.getRecordComponents().length);
    }

    @Test
    @DisplayName("망실 기록이 없는 현황도 0으로 펼친다 — 매핑 경계 방어(NPE 차단)")
    void getSurveyProgress_missingResultKeys_defaultZero() {
        fake.progress = new SurveyProgress("2026 일제조사", 3, 1, 2, false, Map.of(SurveyResult.INTACT, 1L));

        SurveyProgressSummary progress = tools.getSurveyProgress(1L);

        assertEquals(1L, fake.progressProjectId); // 전달받은 id를 그대로 유스케이스에 넘긴다
        assertEquals(0, progress.lostPoints());
        assertEquals(1, progress.intactPoints());
    }

    @Test
    @DisplayName("정상·망실·조사불가·기타·미조사는 겹치지 않고 더하면 대상 전체가 된다")
    void getSurveyProgress_fiveBucketsCoverTotal() {
        fake.progress = new SurveyProgress("굴착협의", 48, 43, 5, false,
                Map.of(SurveyResult.INTACT, 36L, SurveyResult.LOST, 3L, SurveyResult.UNAVAILABLE, 2L, SurveyResult.ETC, 2L));

        SurveyProgressSummary progress = tools.getSurveyProgress(1L);

        assertEquals(36, progress.intactPoints());
        assertEquals(3, progress.lostPoints());
        assertEquals(2, progress.unavailablePoints());
        assertEquals(2, progress.etcPoints());
        assertEquals(5, progress.notSurveyedPoints());
        assertEquals(
                48,
                progress.intactPoints() + progress.lostPoints() + progress.unavailablePoints()
                        + progress.etcPoints() + progress.notSurveyedPoints());
        assertEquals(43, progress.surveyedPoints()); // 조사한 수는 넷의 합이라 미조사까지 함께 더하면 안 된다
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

    @Test
    @DisplayName("조사 기록 — 결과로 거르고 조사일이 늦은 것부터 자르며 기준점 이름을 붙인다")
    void getSurveyRecords_filtersSortsAndJoins() {
        points.points.add(point(1L, "41192D000001265", "1465공"));
        points.points.add(point(2L, "41192D000001266", "1466공"));
        fake.records.add(record(1L, SurveyResult.LOST, "2026-08-10T09:00:00+09:00", "표지 없음"));
        fake.records.add(record(2L, SurveyResult.INTACT, "2026-08-12T09:00:00+09:00", null));

        List<SurveyRecordBrief> all = tools.getSurveyRecords(1L, null, null);
        assertEquals(List.of("1466공", "1465공"), all.stream().map(SurveyRecordBrief::name).toList());
        assertEquals(LocalDate.of(2026, 8, 12), all.getFirst().surveyedOn());
        assertEquals("황인우", all.getFirst().surveyorName());

        List<SurveyRecordBrief> lost = tools.getSurveyRecords(1L, "망실", null);
        assertEquals(1, lost.size());
        assertEquals("41192D000001265", lost.getFirst().pointNo());
        assertEquals("표지 없음", lost.getFirst().note());

        assertEquals(1, tools.getSurveyRecords(1L, null, 1).size());
    }

    private static ControlPoint point(Long id, String pointNo, String name) {
        return ControlPoint.restore(
                id, pointNo, PointType.DOGEUN, name,
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL, new BigDecimal("545236.77"), new BigDecimal("181840.96")),
                new GeoCoordinate(126.794623, 37.506423),
                null, null, null, null, null, null, null, null, null);
    }

    private static SurveyRecordSummary record(Long pointId, SurveyResult result, String surveyedAt, String note) {
        return new SurveyRecordSummary(
                SurveyRecord.restore(1L, pointId, result, OffsetDateTime.parse(surveyedAt), note, 12L),
                "황인우");
    }

    /** 기준점 조회 유스케이스 페이크 — 기록에 붙일 이름만 준다. */
    private static class FakePoints implements GetControlPointsUseCase {

        final List<ControlPoint> points = new ArrayList<>();

        @Override
        public List<ControlPoint> getAll() {
            return points;
        }

        @Override
        public ControlPoint getByPointNo(String pointNo) {
            throw new UnsupportedOperationException("조사 기록 도구는 관리번호로 찾지 않는다");
        }

        @Override
        public com.is.bcs.application.dto.ControlPointCountSummary getCountSummary() {
            throw new UnsupportedOperationException("조사 기록 도구는 개수를 세지 않는다");
        }

        @Override
        public com.is.bcs.application.dto.LastSurveySummary getLastSurvey(Long pointId) {
            throw new UnsupportedOperationException("조사 기록 도구는 최종조사를 읽지 않는다");
        }

        @Override
        public List<com.is.bcs.application.dto.PointLastSurvey> getLastSurveys() {
            throw new UnsupportedOperationException("조사 기록 도구는 점 전체의 최종조사를 읽지 않는다");
        }
    }

    /** 조사 조회 유스케이스 페이크. */
    private static class FakeSurveys implements GetSurveyProjectsUseCase, GetSurveyRecordsUseCase {

        final Map<Long, SurveyProject> projects = new HashMap<>();
        final Map<Long, Long> targetCounts = new HashMap<>();
        final Map<Long, Long> surveyedCounts = new HashMap<>();
        final List<SurveyRecordSummary> records = new ArrayList<>();
        SurveyProgress progress;
        Long progressProjectId; // getProgress에 전달된 id 기록 — 도구가 올바른 id를 넘기는지 검증용

        @Override
        public List<SurveyProject> getAll() {
            return List.copyOf(projects.values());
        }

        @Override
        public List<com.is.bcs.application.dto.SurveyProjectSummary> getSummaries() {
            return projects.values().stream()
                    .map(project -> new com.is.bcs.application.dto.SurveyProjectSummary(
                            project,
                            targetCounts.getOrDefault(project.getId(), 0L),
                            surveyedCounts.getOrDefault(project.getId(), 0L),
                            null))
                    .toList();
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
            return records;
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
