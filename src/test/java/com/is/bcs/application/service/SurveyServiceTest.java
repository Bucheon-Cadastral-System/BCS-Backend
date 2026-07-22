package com.is.bcs.application.service;

import com.is.bcs.application.dto.CreateSurveyProjectCommand;
import com.is.bcs.application.dto.RecordSurveyCommand;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.survey.DeleteSurveyRecordPort;
import com.is.bcs.application.port.out.survey.LoadSurveyProjectPort;
import com.is.bcs.application.port.out.survey.LoadSurveyRecordPort;
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
import com.is.bcs.domain.survey.SurveyProjectType;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import com.is.bcs.domain.survey.exception.SurveyProjectNotFoundException;
import com.is.bcs.domain.survey.exception.SurveyRecordNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurveyServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-22T09:00:00Z");
    private static final OffsetDateTime FIXED_KST = OffsetDateTime.ofInstant(FIXED_INSTANT, TimeConfig.KST);

    private final FakeSurveyStore store = new FakeSurveyStore();
    private final FakePointStore pointStore = new FakePointStore();
    private final SurveyService service = new SurveyService(
            store, store, store, store, store, pointStore, Clock.fixed(FIXED_INSTANT, TimeConfig.KST));

    private SurveyProject excavationProject() {
        return service.create(new CreateSurveyProjectCommand(
                SurveyProjectType.EXCAVATION_CONSULTATION, "2026 굴착협의", "협의번호 2333"));
    }

    @Test
    @DisplayName("프로젝트를 생성하면 id가 발급되고 조회된다")
    void createAndGetProject() {
        SurveyProject created = excavationProject();

        assertNotNull(created.getId());
        assertEquals("2026 굴착협의", service.getById(created.getId()).getName());
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
        SurveyProject project = excavationProject();
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
        SurveyProject project = excavationProject();
        pointStore.add(10L);
        SurveyRecord first = service.record(
                new RecordSurveyCommand(project.getId(), 10L, SurveyResult.INTACT, null));

        SurveyRecord revised = service.record(
                new RecordSurveyCommand(project.getId(), 10L, SurveyResult.LOST, null));

        assertEquals(first.getId(), revised.getId()); // 레코드는 하나 — id 유지
        assertTrue(revised.isLost());
        assertEquals(1, service.getByProjectId(project.getId()).size());
    }

    @Test
    @DisplayName("없는 프로젝트·기준점에는 조사를 기록할 수 없다")
    void record_missingProjectOrPoint_throws() {
        SurveyProject project = excavationProject();
        pointStore.add(10L);

        assertThrows(SurveyProjectNotFoundException.class,
                () -> service.record(new RecordSurveyCommand(99L, 10L, SurveyResult.INTACT, null)));
        assertThrows(ControlPointNotFoundException.class,
                () -> service.record(new RecordSurveyCommand(project.getId(), 99L, SurveyResult.INTACT, null)));
    }

    @Test
    @DisplayName("조사 취소는 레코드를 삭제하고, 없는 기록 취소는 SurveyRecordNotFoundException")
    void cancel_deletesRecord() {
        SurveyProject project = excavationProject();
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

    /** 조사 포트 페이크 — 인메모리 저장으로 서비스 로직만 검증한다. */
    private static class FakeSurveyStore implements LoadSurveyProjectPort, SaveSurveyProjectPort,
            LoadSurveyRecordPort, SaveSurveyRecordPort, DeleteSurveyRecordPort {

        private final Map<Long, SurveyProject> projects = new HashMap<>();
        private final Map<Long, SurveyRecord> records = new HashMap<>();
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
            SurveyProject saved = SurveyProject.restore(id, project.getType(), project.getName(), project.getNote());
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
        public List<ControlPoint> findAll() {
            return new ArrayList<>(points.values());
        }

        @Override
        public boolean existsByPointNo(String pointNo) {
            return findByPointNo(pointNo).isPresent();
        }
    }
}
