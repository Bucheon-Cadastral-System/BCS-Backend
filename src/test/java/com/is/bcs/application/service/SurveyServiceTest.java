package com.is.bcs.application.service;

import com.is.bcs.application.dto.CreateSurveyProjectCommand;
import com.is.bcs.application.dto.RecordSurveyCommand;
import com.is.bcs.application.dto.SurveyProgress;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.survey.DeleteSurveyRecordPort;
import com.is.bcs.application.port.out.survey.LoadSurveyProjectPort;
import com.is.bcs.application.port.out.survey.LoadSurveyRecordPort;
import com.is.bcs.application.port.out.survey.LoadSurveyTargetPort;
import com.is.bcs.application.port.out.survey.SaveSurveyProjectPort;
import com.is.bcs.application.port.out.survey.SaveSurveyRecordPort;
import com.is.bcs.config.TimeConfig;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpoint.exception.ControlPointNotFoundException;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import com.is.bcs.domain.survey.SurveyTarget;
import com.is.bcs.domain.survey.exception.SurveyProjectNotFoundException;
import com.is.bcs.domain.survey.exception.SurveyRecordNotFoundException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurveyServiceTest {

    private static final LocalDate STARTED = LocalDate.of(2026, 7, 1);

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-22T09:00:00Z");
    private static final OffsetDateTime FIXED_KST = OffsetDateTime.ofInstant(FIXED_INSTANT, TimeConfig.KST);

    private final FakeSurveyStore store = new FakeSurveyStore();
    private final FakePointStore pointStore = new FakePointStore();
    private final SurveyService service = new SurveyService(
            store, store, store, store, store, store, pointStore, Clock.fixed(FIXED_INSTANT, TimeConfig.KST));

    private SurveyProject sampleProject() {
        return service.create(new CreateSurveyProjectCommand("2026 일제조사", STARTED, null, "정기 조사"));
    }

    @Test
    @DisplayName("조사 대상 점 id — 그 프로젝트의 대상만 돌려주고 없는 프로젝트는 거부한다")
    void getTargetPointIds() {
        SurveyProject project = sampleProject();
        SurveyProject other = service.create(new CreateSurveyProjectCommand("다른 조사", STARTED, null, null));
        store.targets.add(SurveyTarget.create(project.getId(), 10L));
        store.targets.add(SurveyTarget.create(project.getId(), 11L));
        store.targets.add(SurveyTarget.create(other.getId(), 12L));

        assertEquals(List.of(10L, 11L), service.getTargetPointIds(project.getId()));
        assertThrows(SurveyProjectNotFoundException.class, () -> service.getTargetPointIds(999L));
    }

    @Test
    @DisplayName("프로젝트를 생성하면 id가 발급되고 조회된다")
    void createAndGetProject() {
        SurveyProject created = sampleProject();

        assertNotNull(created.getId());
        assertEquals("2026 일제조사", service.getById(created.getId()).getName());
        assertEquals(1, service.getAll().size());
    }

    @Test
    @DisplayName("없는 프로젝트 조회는 SurveyProjectNotFoundException")
    void getProject_notFound_throws() {
        assertThrows(SurveyProjectNotFoundException.class, () -> service.getById(99L));
    }

    @Test
    @DisplayName("조사를 기록하면 조사 시각이 Clock(KST)으로 찍힌 레코드가 생긴다")
    void record_createsRecordWithClockTime() {
        SurveyProject project = sampleProject();
        pointStore.add(10L);

        SurveyRecord record = service.record(
                new RecordSurveyCommand(project.getId(), 10L, SurveyResult.INTACT, "대상(2건)"));

        assertNotNull(record.getId());
        assertEquals(FIXED_KST, record.getSurveyedAt());
        assertEquals(SurveyResult.INTACT, record.getResult());
        assertEquals(1, service.getByProjectId(project.getId()).size());
    }

    @Test
    @DisplayName("같은 프로젝트×기준점에 다시 기록하면 새 레코드가 아니라 판정 정정이다")
    void record_existing_revisesInsteadOfDuplicate() {
        SurveyProject project = sampleProject();
        pointStore.add(10L);
        SurveyRecord first = service.record(
                new RecordSurveyCommand(project.getId(), 10L, SurveyResult.INTACT, null));

        SurveyRecord revised = service.record(
                new RecordSurveyCommand(project.getId(), 10L, SurveyResult.LOST, "정정 비고"));

        assertEquals(first.getId(), revised.getId()); // 레코드는 하나 — id 유지
        assertTrue(revised.isLost());
        assertEquals("정정 비고", revised.getNote()); // 정정 시 비고도 교체된다
        assertEquals(1, service.getByProjectId(project.getId()).size());
    }

    @Test
    @DisplayName("없는 프로젝트·기준점에는 조사를 기록할 수 없다")
    void record_missingProjectOrPoint_throws() {
        SurveyProject project = sampleProject();
        pointStore.add(10L);

        assertThrows(SurveyProjectNotFoundException.class,
                () -> service.record(new RecordSurveyCommand(99L, 10L, SurveyResult.INTACT, null)));
        assertThrows(ControlPointNotFoundException.class,
                () -> service.record(new RecordSurveyCommand(project.getId(), 99L, SurveyResult.INTACT, null)));
    }

    @Test
    @DisplayName("조사 취소는 레코드를 삭제하고, 없는 기록 취소는 SurveyRecordNotFoundException")
    void cancel_deletesRecord() {
        SurveyProject project = sampleProject();
        pointStore.add(10L);
        service.record(new RecordSurveyCommand(project.getId(), 10L, SurveyResult.INTACT, null));

        service.cancel(project.getId(), 10L);

        assertEquals(0, service.getByProjectId(project.getId()).size());
        assertThrows(SurveyRecordNotFoundException.class, () -> service.cancel(project.getId(), 10L));
    }

    @Test
    @DisplayName("없는 프로젝트의 조사기록 목록 조회는 SurveyProjectNotFoundException")
    void getRecords_missingProject_throws() {
        assertThrows(SurveyProjectNotFoundException.class, () -> service.getByProjectId(99L));
    }

    @Test
    @DisplayName("조사 현황 — 조사됨=기록 존재(망실 포함), 결과별 개수는 없는 결과도 0으로 채워 준다")
    void getProgress_countsRecordsByResult() {
        SurveyProject project = sampleProject();
        for (long i = 10; i <= 14; i++) {
            pointStore.add(i);
            store.targets.add(SurveyTarget.create(project.getId(), i));
        }
        service.record(new RecordSurveyCommand(project.getId(), 10L, SurveyResult.INTACT, null));
        service.record(new RecordSurveyCommand(project.getId(), 11L, SurveyResult.LOST, null));
        service.record(new RecordSurveyCommand(project.getId(), 12L, SurveyResult.INTACT, null));

        SurveyProgress progress = service.getProgress(project.getId());

        assertEquals("2026 일제조사", progress.projectName());
        assertEquals(5, progress.totalPoints());
        assertEquals(3, progress.surveyedPoints());
        assertEquals(2, progress.notSurveyedPoints());
        assertEquals(2, progress.countByResult().get(SurveyResult.INTACT));
        assertEquals(1, progress.countByResult().get(SurveyResult.LOST));
        assertEquals(0, progress.countByResult().get(SurveyResult.ETC));
        assertFalse(progress.complete()); // 3/5 조사 — 미완
    }

    @Test
    @DisplayName("조사 현황 — 기록이 하나도 없는 프로젝트는 조사됨 0, 결과별 개수도 모두 0으로 채운다")
    void getProgress_noRecords_fillsZeros() {
        SurveyProject project = sampleProject();
        pointStore.add(10L);
        pointStore.add(11L);
        store.targets.add(SurveyTarget.create(project.getId(), 10L));
        store.targets.add(SurveyTarget.create(project.getId(), 11L));

        SurveyProgress progress = service.getProgress(project.getId());

        assertEquals(2, progress.totalPoints());
        assertEquals(0, progress.surveyedPoints());
        assertEquals(2, progress.notSurveyedPoints());
        assertEquals(0, progress.countByResult().get(SurveyResult.INTACT));
        assertEquals(0, progress.countByResult().get(SurveyResult.LOST));
        assertEquals(0, progress.countByResult().get(SurveyResult.ETC));
        assertFalse(progress.complete()); // 대상 2, 조사 0 — 미완
    }

    @Test
    @DisplayName("없는 프로젝트의 조사 현황 조회는 SurveyProjectNotFoundException")
    void getProgress_missingProject_throws() {
        assertThrows(SurveyProjectNotFoundException.class, () -> service.getProgress(99L));
    }

    @Test
    @DisplayName("진행률의 전체(total)는 전역 기준점 수가 아니라 프로젝트의 대상 점 수다")
    void getProgress_totalIsProjectTargetCount() {
        SurveyProject project = sampleProject();
        Long pid = project.getId();
        // 전역 기준점은 10개지만, 이 프로젝트의 조사 대상은 4개
        for (long i = 1; i <= 10; i++) {
            pointStore.add(i);
        }
        for (long i = 1; i <= 4; i++) {
            store.targets.add(SurveyTarget.create(pid, i));
        }
        service.record(new RecordSurveyCommand(pid, 1L, SurveyResult.INTACT, null));
        service.record(new RecordSurveyCommand(pid, 2L, SurveyResult.LOST, null));

        SurveyProgress progress = service.getProgress(pid);

        assertEquals(4, progress.totalPoints());       // 전역 10이 아니라 대상 4
        assertEquals(2, progress.surveyedPoints());
        assertEquals(2, progress.notSurveyedPoints()); // 4 - 2
        assertFalse(progress.complete()); // 2/4 조사 — 미완
    }

    @Test
    @DisplayName("대상이 전부 조사되면 complete=true")
    void getProgress_allTargetsSurveyed_complete() {
        SurveyProject project = sampleProject();
        Long pid = project.getId();
        for (long i = 1; i <= 2; i++) {
            pointStore.add(i);
            store.targets.add(SurveyTarget.create(pid, i));
        }
        service.record(new RecordSurveyCommand(pid, 1L, SurveyResult.INTACT, null));
        service.record(new RecordSurveyCommand(pid, 2L, SurveyResult.LOST, null));

        SurveyProgress progress = service.getProgress(pid);

        assertTrue(progress.complete());
        assertEquals(0, progress.notSurveyedPoints());
    }

    @Test
    @DisplayName("대상이 아닌 점의 기록은 진행률에 안 들어간다 (오탐 완료 방지)")
    void getProgress_ignoresNonTargetRecords() {
        SurveyProject project = sampleProject();
        Long pid = project.getId();
        pointStore.add(1L);
        pointStore.add(2L);
        store.targets.add(SurveyTarget.create(pid, 1L)); // 대상은 1번만
        service.record(new RecordSurveyCommand(pid, 2L, SurveyResult.INTACT, null)); // 비대상 2번에 기록

        SurveyProgress progress = service.getProgress(pid);

        assertEquals(1, progress.totalPoints());    // 대상 1
        assertEquals(0, progress.surveyedPoints());  // 비대상 기록은 미집계
        assertFalse(progress.complete());            // 대상이 미조사라 미완
    }

    /** 조사 포트 페이크 — 인메모리 저장으로 서비스 로직만 검증한다. */
    private static class FakeSurveyStore implements LoadSurveyProjectPort, SaveSurveyProjectPort,
            LoadSurveyRecordPort, SaveSurveyRecordPort, DeleteSurveyRecordPort, LoadSurveyTargetPort {

        private final Map<Long, SurveyProject> projects = new HashMap<>();
        private final Map<Long, SurveyRecord> records = new HashMap<>();
        final List<SurveyTarget> targets = new ArrayList<>();
        private long projectSeq = 0;
        private long recordSeq = 0;

        @Override
        public Optional<SurveyProject> findProjectById(Long id) {
            return Optional.ofNullable(projects.get(id));
        }

        @Override
        public List<SurveyProject> findAllProjects() {
            return new ArrayList<>(projects.values());
        }

        @Override
        public SurveyProject save(SurveyProject project) {
            long id = project.getId() != null ? project.getId() : ++projectSeq;
            SurveyProject saved = SurveyProject.restore(id, project.getAuthorId(), project.getName(), project.getStartedOn(), project.getEndedOn(), project.getNote());
            projects.put(id, saved);
            return saved;
        }

        @Override
        public List<SurveyRecord> findRecordsByProjectId(Long projectId) {
            return records.values().stream().filter(r -> r.getProjectId().equals(projectId)).toList();
        }

        @Override
        public Optional<SurveyRecord> findRecordByProjectIdAndPointId(Long projectId, Long pointId) {
            return records.values().stream()
                    .filter(r -> r.getProjectId().equals(projectId) && r.getPointId().equals(pointId))
                    .findFirst();
        }

        @Override
        public Map<SurveyResult, Long> countByResult(Long projectId) {
            Set<Long> targetPoints = targets.stream()
                    .filter(t -> t.getProjectId().equals(projectId))
                    .map(SurveyTarget::getPointId).collect(Collectors.toSet());
            Map<SurveyResult, Long> counts = new HashMap<>();
            records.values().stream()
                    .filter(r -> r.getProjectId().equals(projectId) && targetPoints.contains(r.getPointId()))
                    .forEach(r -> counts.merge(r.getResult(), 1L, Long::sum));
            return counts;
        }

        @Override
        public SurveyRecord save(SurveyRecord record) {
            long id = record.getId() != null ? record.getId() : ++recordSeq;
            SurveyRecord saved = SurveyRecord.restore(
                    id, record.getProjectId(), record.getPointId(),
                    record.getResult(), record.getSurveyedAt(), record.getNote());
            records.put(id, saved);
            return saved;
        }

        @Override
        public List<SurveyRecord> saveAll(List<SurveyRecord> list) {
            return list.stream().map(this::save).toList();
        }

        @Override
        public void deleteByProjectIdAndPointId(Long projectId, Long pointId) {
            records.values().removeIf(
                    r -> r.getProjectId().equals(projectId) && r.getPointId().equals(pointId));
        }

        @Override
        public long countByProjectId(Long projectId) {
            return targets.stream().filter(t -> t.getProjectId().equals(projectId)).count();
        }

        @Override
        public List<Long> findPointIdsByProjectId(Long projectId) {
            return targets.stream()
                    .filter(t -> t.getProjectId().equals(projectId))
                    .map(SurveyTarget::getPointId)
                    .toList();
        }
    }

    /** 기준점 존재 확인용 페이크. */
    private static class FakePointStore implements LoadControlPointPort {

        private final Map<Long, ControlPoint> points = new HashMap<>();

        void add(Long id) {
            points.put(id, ControlPoint.restore(
                    id, "41192D%012d".formatted(id), PointType.DOGEUN, "점" + id,
                    new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                            new BigDecimal("545000.00"), new BigDecimal("181000.00")),
                    new GeoCoordinate(126.79, 37.50),
                    null, null, null, null, null, null, null));
        }

        @Override
        public Optional<ControlPoint> findById(Long id) {
            return Optional.ofNullable(points.get(id));
        }

        @Override
        public Optional<ControlPoint> findByPointNo(String pointNo) {
            return points.values().stream().filter(p -> p.getPointNo().equals(pointNo)).findFirst();
        }

        @Override
        public Optional<ControlPoint> findByNameAndType(String name, PointType type) {
            return points.values().stream().filter(p -> p.getName().equals(name) && p.getType() == type).findFirst();
        }

        @Override
        public List<ControlPoint> findAllByNameInOrPointNoIn(
                Collection<String> names, Collection<String> pointNos) {
            return points.values().stream()
                    .filter(p -> names.contains(p.getName()) || pointNos.contains(p.getPointNo()))
                    .toList();
        }

        @Override
        public List<ControlPoint> findAll() {
            return new ArrayList<>(points.values());
        }

        @Override
        public boolean existsByPointNo(String pointNo) {
            return findByPointNo(pointNo).isPresent();
        }

        @Override
        public long count() {
            return points.size();
        }

        @Override
        public Map<PointType, Long> countByType() {
            Map<PointType, Long> counts = new HashMap<>();
            points.values().forEach(p -> counts.merge(p.getType(), 1L, Long::sum));
            return counts;
        }
    }
}
