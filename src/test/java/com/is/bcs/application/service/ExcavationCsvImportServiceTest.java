package com.is.bcs.application.service;

import com.is.bcs.application.dto.ExcavationImportResult;
import com.is.bcs.application.dto.ImportExcavationCsvCommand;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.application.port.out.survey.SaveSurveyProjectPort;
import com.is.bcs.application.port.out.survey.SaveSurveyRecordPort;
import com.is.bcs.application.port.out.survey.SaveSurveyTargetPort;
import com.is.bcs.config.TimeConfig;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpoint.TraverseInfo;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyProjectType;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import com.is.bcs.domain.survey.SurveyTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 굴착협의 CSV 임포트 검증 — 픽스처는 고객사 실파일(49행·기존조사 44건). */
class ExcavationCsvImportServiceTest {

    private final FakeControlPointStore pointStore = new FakeControlPointStore();
    private final FakeSurveyStore surveyStore = new FakeSurveyStore();
    private final FakeTargetStore targetStore = new FakeTargetStore();
    private final ExcavationCsvImportService service = new ExcavationCsvImportService(
            pointStore, pointStore, surveyStore, surveyStore, targetStore,
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
                new ImportExcavationCsvCommand(SurveyProjectType.EXCAVATION_CONSULTATION, "2026 굴착협의", "협의번호 2333", sampleCsv()));

        assertEquals(49, result.totalRows());
        assertEquals(49, result.newPoints());
        assertEquals(0, result.existingPoints());
        assertEquals(0, result.updatedPoints());
        assertEquals(44, result.createdRecords());

        SurveyProject project = surveyStore.projects.get(result.projectId());
        assertEquals(SurveyProjectType.EXCAVATION_CONSULTATION, project.getType());
        assertEquals("2026 굴착협의", project.getName());

        assertEquals(49, pointStore.points.size());
        assertEquals(44, surveyStore.records.size());
    }

    @Test
    @DisplayName("프로젝트 유형은 요청한 값을 따른다 — 같은 서식이라도 일반 조사로 임포트할 수 있다")
    void importCsv_usesRequestedProjectType() throws Exception {
        ExcavationImportResult result = service.importCsv(
                new ImportExcavationCsvCommand(SurveyProjectType.GENERAL, "2026 정기조사", null, sampleCsv()));

        assertEquals(SurveyProjectType.GENERAL, surveyStore.projects.get(result.projectId()).getType());
    }

    @Test
    @DisplayName("조사기록의 시각은 기존조사일의 KST 자정이고 결과·비고가 보존된다")
    void importCsv_recordUsesPriorSurveyDate() throws Exception {
        ExcavationImportResult result = service.importCsv(
                new ImportExcavationCsvCommand(SurveyProjectType.EXCAVATION_CONSULTATION, "2026 굴착협의", null, sampleCsv()));

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
        service.importCsv(new ImportExcavationCsvCommand(SurveyProjectType.EXCAVATION_CONSULTATION, "1차", null, sampleCsv()));

        ExcavationImportResult second = service.importCsv(
                new ImportExcavationCsvCommand(SurveyProjectType.EXCAVATION_CONSULTATION, "2차", null, sampleCsv()));

        assertEquals(0, second.newPoints());
        assertEquals(49, second.existingPoints());
        assertEquals(0, second.updatedPoints()); // 관리번호가 이미 CSV라 갱신 없이 재사용
        assertEquals(49, pointStore.points.size()); // 마스터 중복 생성 없음
        assertTrue(surveyStore.projects.size() == 2);
    }

    @Test
    @DisplayName("이름·종류가 같은 기존 점이 있으면 중복 생성하지 않고 성과·관리번호를 CSV로 갱신한다")
    void importCsv_dedupsByNameAndType_andUpdatesToCsv() throws Exception {
        // 시드 placeholder — 같은 도근점 '4012공'인데 관리번호(짝수)·성과가 CSV(홀수)와 미세하게 다르다
        ControlPoint seed = pointStore.save(ControlPoint.register(
                "41192D000006846", PointType.DOGEUN, "4012공",
                new TmCoordinate(CoordinateSystem.BESSEL_CENTRAL, new BigDecimal("545860.00"), new BigDecimal("177390.00")),
                new GeoCoordinate(126.744200, 37.511900),
                "10900", "상동", "부천시 상동 529-2",
                MarkerMaterial.STEEL, InstallType.INSTALLED, LocalDate.parse("2020-07-27"),
                new TraverseInfo("2", "ㅁ", "78", false)));

        ExcavationImportResult result = service.importCsv(new ImportExcavationCsvCommand(SurveyProjectType.EXCAVATION_CONSULTATION, "2026 굴착협의", null, sampleCsv()));
        assertEquals(1, result.updatedPoints()); // 시드 쌍둥이 1건만 갱신
        assertEquals(48, result.newPoints());    // 나머지 48건은 신규

        // '4012공'은 하나만 — 시드 쌍둥이가 갱신될 뿐 새로 추가되지 않는다
        List<ControlPoint> named = pointStore.points.values().stream()
                .filter(p -> "4012공".equals(p.getName())).toList();
        assertEquals(1, named.size());

        ControlPoint merged = named.get(0);
        assertEquals(seed.getId(), merged.getId()); // 기존 점 id 보존(삭제·재생성이 아님)
        assertEquals("41192D000006847", merged.getPointNo()); // 관리번호가 CSV(홀수)로 갱신
        assertEquals(CoordinateSystem.GRS80_CENTRAL, merged.getTm().crs()); // 좌표계도 세계측지계로
        assertEquals(0, new BigDecimal("545860.82").compareTo(merged.getTm().northing()));
        assertEquals(0, new BigDecimal("177390.84").compareTo(merged.getTm().easting()));
        assertEquals(126.744273, merged.getGeo().longitude(), 1e-6);
        assertEquals(37.511947, merged.getGeo().latitude(), 1e-6);

        assertTrue(pointStore.findByPointNo("41192D000006846").isEmpty()); // 옛 관리번호는 사라짐
        assertEquals(49, pointStore.points.size()); // 시드 1 + 신규 48 = 49 (중복 아님)
    }

    @Test
    @DisplayName("관리번호가 같아도 CSV 성과가 다르면 갱신한다")
    void importCsv_samePointNoButChangedCoordinates_updates() throws Exception {
        // 같은 도근점 '4012공'·같은 관리번호(홀수)인데 좌표가 옛 값
        pointStore.save(ControlPoint.register(
                "41192D000006847", PointType.DOGEUN, "4012공",
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL, new BigDecimal("545000.00"), new BigDecimal("177000.00")),
                new GeoCoordinate(126.744000, 37.511000),
                "10900", "상동", "부천시 상동 529-2",
                MarkerMaterial.STEEL, InstallType.INSTALLED, LocalDate.parse("2020-07-27"),
                new TraverseInfo("2", "ㅁ", "78", false)));

        ExcavationImportResult result = service.importCsv(
                new ImportExcavationCsvCommand(SurveyProjectType.EXCAVATION_CONSULTATION, "2026 굴착협의", null, sampleCsv()));

        assertEquals(1, result.updatedPoints()); // 관리번호가 같아도 성과가 달라 갱신
        ControlPoint merged = pointStore.points.values().stream()
                .filter(p -> "4012공".equals(p.getName())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("545860.82").compareTo(merged.getTm().northing())); // CSV 좌표로 갱신됨
    }

    @Test
    @DisplayName("임포트하면 모든 행이 프로젝트의 조사 대상으로 등록된다")
    void importCsv_registersAllRowsAsTargets() throws Exception {
        ExcavationImportResult result = service.importCsv(
                new ImportExcavationCsvCommand(SurveyProjectType.EXCAVATION_CONSULTATION, "2026 굴착협의", null, sampleCsv()));

        assertEquals(49, targetStore.targets.size());
        assertTrue(targetStore.targets.stream().allMatch(t -> t.getProjectId().equals(result.projectId())));

        Set<Long> targetPointIds = targetStore.targets.stream().map(SurveyTarget::getPointId).collect(Collectors.toSet());
        assertEquals(49, targetPointIds.size()); // 대상 pointId에 중복 없음
        assertTrue(targetPointIds.stream().allMatch(id -> pointStore.findById(id).isPresent())); // 실제 임포트된 기준점에 대응
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
        public Optional<ControlPoint> findByNameAndType(String name, PointType type) {
            return points.values().stream()
                    .filter(p -> p.getName().equals(name) && p.getType() == type)
                    .findFirst();
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

    /** 조사 대상 포트 페이크. */
    private static class FakeTargetStore implements SaveSurveyTargetPort {

        final List<SurveyTarget> targets = new ArrayList<>();
        private long sequence = 0;

        @Override
        public SurveyTarget save(SurveyTarget target) {
            SurveyTarget saved = SurveyTarget.restore(++sequence, target.getProjectId(), target.getPointId());
            targets.add(saved);
            return saved;
        }

        @Override
        public List<SurveyTarget> saveAll(List<SurveyTarget> list) {
            return list.stream().map(this::save).toList();
        }
    }
}
