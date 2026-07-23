package com.is.bcs.application.service;

import com.is.bcs.application.dto.ExcavationImportResult;
import com.is.bcs.application.dto.ImportExcavationCsvCommand;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.application.port.out.survey.SaveSurveyProjectPort;
import com.is.bcs.application.port.out.survey.SaveSurveyRecordPort;
import com.is.bcs.config.TimeConfig;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyProjectType;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 굴착협의 CSV 임포트 검증 — 픽스처는 고객사 실파일(49행·기존조사 44건). */
class ExcavationCsvImportServiceTest {

    private final FakeControlPointStore pointStore = new FakeControlPointStore();
    private final FakeSurveyStore surveyStore = new FakeSurveyStore();
    private final ExcavationCsvImportService service = new ExcavationCsvImportService(
            pointStore, pointStore, surveyStore, surveyStore,
            Clock.fixed(Instant.parse("2026-07-22T09:00:00Z"), TimeConfig.KST));

    private byte[] sampleCsv() throws Exception {
        try (var in = getClass().getResourceAsStream("/excavation-sample.csv")) {
            return in.readAllBytes();
        }
    }

    @Test
    @DisplayName("임포트 — 굴착협의 프로젝트가 생기고 기준점 49개 등록, 기존조사 44건이 기록된다")
    void importCsv_createsProjectPointsAndRecords() throws Exception {
        ExcavationImportResult result = service.importCsv(
                new ImportExcavationCsvCommand("2026 굴착협의", "협의번호 2333", sampleCsv()));

        assertEquals(49, result.totalRows());
        assertEquals(49, result.newPoints());
        assertEquals(0, result.existingPoints());
        assertEquals(44, result.createdRecords());

        SurveyProject project = surveyStore.projects.get(result.projectId());
        assertEquals(SurveyProjectType.EXCAVATION_CONSULTATION, project.getType());
        assertEquals("2026 굴착협의", project.getName());

        assertEquals(49, pointStore.points.size());
        assertEquals(44, surveyStore.records.size());
    }

    @Test
    @DisplayName("조사기록의 시각은 기존조사일의 KST 자정이고 결과·비고가 보존된다")
    void importCsv_recordUsesPriorSurveyDate() throws Exception {
        ExcavationImportResult result = service.importCsv(
                new ImportExcavationCsvCommand("2026 굴착협의", null, sampleCsv()));

        ControlPoint row1Point = pointStore.findByPointNo("41192D000001265").orElseThrow();
        SurveyRecord record = surveyStore.records.values().stream()
                .filter(r -> r.getPointId().equals(row1Point.getId()))
                .findFirst().orElseThrow();

        assertEquals(SurveyResult.INTACT, record.getResult());
        assertEquals(OffsetDateTime.parse("2025-09-08T00:00:00+09:00"), record.getSurveyedAt());
        assertEquals("대상", record.getNote());
        assertEquals(result.projectId(), record.getProjectId());
    }

    @Test
    @DisplayName("재임포트하면 기준점은 전부 기존으로 집계되고 마스터는 다시 만들지 않는다")
    void importCsv_again_reusesExistingPoints() throws Exception {
        service.importCsv(new ImportExcavationCsvCommand("1차", null, sampleCsv()));

        ExcavationImportResult second = service.importCsv(
                new ImportExcavationCsvCommand("2차", null, sampleCsv()));

        assertEquals(0, second.newPoints());
        assertEquals(49, second.existingPoints());
        assertEquals(49, pointStore.points.size()); // 마스터 중복 생성 없음
        assertTrue(surveyStore.projects.size() == 2);
    }

    /** 기준점 포트 페이크. */
    private static class FakeControlPointStore implements LoadControlPointPort, SaveControlPointPort {

        final Map<Long, ControlPoint> points = new HashMap<>();
        private long sequence = 0;

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

        @Override
        public ControlPoint save(ControlPoint point) {
            long id = point.getId() != null ? point.getId() : ++sequence;
            ControlPoint saved = ControlPoint.restore(
                    id, point.getPointNo(), point.getType(), point.getName(),
                    point.getTm(), point.getGeo(),
                    point.getRegionCode(), point.getRegionName(), point.getAddress(),
                    point.getMarkerMaterial(), point.getInstallType(), point.getInstalledDate(),
                    point.getTraverse());
            points.put(id, saved);
            return saved;
        }

        @Override
        public List<ControlPoint> saveAll(List<ControlPoint> list) {
            return list.stream().map(this::save).toList();
        }
    }

    /** 조사 포트 페이크. */
    private static class FakeSurveyStore implements SaveSurveyProjectPort, SaveSurveyRecordPort {

        final Map<Long, SurveyProject> projects = new HashMap<>();
        final Map<Long, SurveyRecord> records = new HashMap<>();
        private long projectSeq = 0;
        private long recordSeq = 0;

        @Override
        public SurveyProject save(SurveyProject project) {
            long id = project.getId() != null ? project.getId() : ++projectSeq;
            SurveyProject saved = SurveyProject.restore(id, project.getType(), project.getName(), project.getNote());
            projects.put(id, saved);
            return saved;
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
    }
}
