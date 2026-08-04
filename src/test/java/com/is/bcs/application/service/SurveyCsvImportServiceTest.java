package com.is.bcs.application.service;

import com.is.bcs.adapter.out.file.SpreadsheetTableExtractor;
import com.is.bcs.adapter.out.geo.Proj4jCoordinateTransformer;
import com.is.bcs.application.dto.SurveyCsvImportResult;
import com.is.bcs.application.dto.ImportSurveyCsvCommand;
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
import com.is.bcs.domain.controlpoint.exception.DuplicateControlPointException;
import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import com.is.bcs.domain.survey.ExtraColumn;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import com.is.bcs.domain.survey.SurveyTarget;
import com.is.bcs.support.FakeControlPointStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 대상지 CSV 임포트 검증 — 픽스처는 고객사 실파일(49행·기존조사 44건). */
class SurveyCsvImportServiceTest {

    private static final LocalDate STARTED = LocalDate.of(2026, 7, 1);

    private final FakeControlPointStore pointStore = new FakeControlPointStore();
    private final FakeSurveyStore surveyStore = new FakeSurveyStore();
    private final FakeTargetStore targetStore = new FakeTargetStore();
    // 변환기는 입출력이 없는 순수 계산이라 실제 구현을 쓴다 — 파생된 경위도가 실제 값인지까지 확인된다
    private final SurveyCsvImportService service = new SurveyCsvImportService(
            surveyStore, surveyStore, targetStore, new SpreadsheetTableExtractor(),
            new SurveyTargetMapper(new Proj4jCoordinateTransformer()),
            new ControlPointRegistrar(pointStore, pointStore), directTransaction(),
            Clock.fixed(Instant.parse("2026-07-22T09:00:00Z"), TimeConfig.KST));

    /** 페이크 저장소에는 트랜잭션이 없다 — 경계만 통과시키고 커밋·롤백은 하지 않는다. */
    private static TransactionTemplate directTransaction() {
        return new TransactionTemplate(new PlatformTransactionManager() {

            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        });
    }

    private byte[] sampleCsv() throws Exception {
        try (var in = getClass().getResourceAsStream("/survey-target-sample.csv")) {
            return in.readAllBytes();
        }
    }

    @Test
    @DisplayName("임포트 — 조사 프로젝트가 생기고 기준점 49개 등록, 기존조사 44건이 기록된다")
    void importCsv_createsProjectPointsAndRecords() throws Exception {
        SurveyCsvImportResult result = service.importCsv(
                new ImportSurveyCsvCommand("2026 일제조사", STARTED, null, "정기 조사", sampleCsv()));

        assertEquals(49, result.totalRows());
        assertEquals(49, result.newPoints());
        assertEquals(0, result.existingPoints());
        assertEquals(0, result.updatedPoints());
        assertEquals(44, result.createdRecords());

        SurveyProject project = surveyStore.projects.get(result.projectId());
        assertEquals(STARTED, project.getStartedOn());
        assertEquals("2026 일제조사", project.getName());

        assertEquals(49, pointStore.points.size());
        assertEquals(44, surveyStore.records.size());
    }

    @Test
    @DisplayName("조사기록의 시각은 기존조사일의 KST 자정이고 결과·비고가 보존된다")
    void importCsv_recordUsesPriorSurveyDate() throws Exception {
        SurveyCsvImportResult result = service.importCsv(
                new ImportSurveyCsvCommand("2026 일제조사", STARTED, null, null, sampleCsv()));

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
        service.importCsv(new ImportSurveyCsvCommand("1차", STARTED, null, null, sampleCsv()));

        SurveyCsvImportResult second = service.importCsv(
                new ImportSurveyCsvCommand("2차", STARTED, null, null, sampleCsv()));

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
                new TraverseInfo("2", "ㅁ", "78", false), null, null, null));

        SurveyCsvImportResult result = service.importCsv(new ImportSurveyCsvCommand("2026 일제조사", STARTED, null, null, sampleCsv()));
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
                new TraverseInfo("2", "ㅁ", "78", false), null, null, null));

        SurveyCsvImportResult result = service.importCsv(
                new ImportSurveyCsvCommand("2026 일제조사", STARTED, null, null, sampleCsv()));

        assertEquals(1, result.updatedPoints()); // 관리번호가 같아도 성과가 달라 갱신
        ControlPoint merged = pointStore.points.values().stream()
                .filter(p -> "4012공".equals(p.getName())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("545860.82").compareTo(merged.getTm().northing())); // CSV 좌표로 갱신됨
    }

    @Test
    @DisplayName("경위도 열이 없는 기본 양식도 성과 좌표에서 파생한 경위도로 등록된다")
    void importCsv_basicForm_derivesGeoFromTm() throws Exception {
        byte[] basicCsv;
        try (var in = getClass().getResourceAsStream("/survey-target-basic.csv")) {
            basicCsv = in.readAllBytes();
        }

        service.importCsv(new ImportSurveyCsvCommand("기본 양식 조사", STARTED, null, null, basicCsv));

        ControlPoint first = pointStore.findByPointNo("41192D000001265").orElseThrow();
        // 기준값은 경위도 열이 덧붙은 확장 양식(/survey-target-sample.csv)의 같은 행에 적힌 값이다
        assertEquals(126.794623, first.getGeo().longitude(), 1e-6);
        assertEquals(37.506423, first.getGeo().latitude(), 1e-6);
    }

    @Test
    @DisplayName("임포트하면 모든 행이 프로젝트의 조사 대상으로 등록된다")
    void importCsv_registersAllRowsAsTargets() throws Exception {
        SurveyCsvImportResult result = service.importCsv(
                new ImportSurveyCsvCommand("2026 일제조사", STARTED, null, null, sampleCsv()));

        assertEquals(49, targetStore.targets.size());
        assertTrue(targetStore.targets.stream().allMatch(t -> t.getProjectId().equals(result.projectId())));

        Set<Long> targetPointIds = targetStore.targets.stream().map(SurveyTarget::getPointId).collect(Collectors.toSet());
        assertEquals(49, targetPointIds.size()); // 대상 pointId에 중복 없음
        assertTrue(targetPointIds.stream().allMatch(id -> pointStore.findById(id).isPresent())); // 실제 임포트된 기준점에 대응
    }

    @Test
    @DisplayName("같은 기준점이 두 번 있는 파일은 저장을 시작하기 전에 거부한다")
    void importCsv_duplicatePointInFile_rejectsWholeFile() {
        byte[] csv = """
                기준점번호,종류,기준점명,좌표계구분,X좌표,Y좌표,조사대상여부
                41192D000000001,도근점,1465공,세계,545236.77,181840.96,대상
                41192D000000002,도근점,1465공,세계,545000.00,181000.00
                """.getBytes(StandardCharsets.UTF_8);

        assertThrows(InvalidControlPointException.class, () -> service.importCsv(
                new ImportSurveyCsvCommand("중복 조사", STARTED, null, null, csv)));

        // 매퍼가 행 오류로 걸러 트랜잭션에 들어가기 전에 거부하므로 저장이 아예 시작되지 않는다
        assertTrue(pointStore.points.isEmpty());
        assertTrue(targetStore.targets.isEmpty());
        assertTrue(surveyStore.projects.isEmpty());
    }

    @Test
    @DisplayName("중복으로 거부한 행의 값이 뒤 행의 판정을 오염시키지 않는다")
    void importCsv_rejectedRowDoesNotBlockLaterRow() throws Exception {
        // 행 번호는 오류 메시지와 같게 헤더를 1행으로 센다.
        // 3행은 관리번호가 2행과 겹쳐 거부되고, 4행은 그 3행과 이름만 같을 뿐 관리번호가 고유하다.
        byte[] csv = """
                기준점번호,종류,기준점명,좌표계구분,X좌표,Y좌표,조사대상여부
                41192D000000001,도근점,1465공,세계,545236.77,181840.96,대상
                41192D000000001,도근점,1466공,세계,545100.00,181100.00
                41192D000000002,도근점,1466공,세계,545200.00,181200.00,대상
                """.getBytes(StandardCharsets.UTF_8);

        InvalidControlPointException thrown = assertThrows(InvalidControlPointException.class,
                () -> service.importCsv(new ImportSurveyCsvCommand("중복 조사", STARTED, null, null, csv)));

        // 거부는 3행 하나뿐이어야 한다 — 4행까지 '같은 기준점'으로 걸리면 담당자가 멀쩡한 행을 고치게 된다
        assertTrue(thrown.getMessage().contains("3행"), thrown.getMessage());
        assertTrue(!thrown.getMessage().contains("4행"), thrown.getMessage());
    }

    @Test
    @DisplayName("이미 다른 점이 쓰는 관리번호면 거부한다 — 저장 제약에 걸려 서버 오류로 새지 않게")
    void importCsv_pointNoTakenByAnotherPoint_isRejected() {
        pointStore.save(ControlPoint.register(
                "41192D000000001", PointType.DOGEUN, "다른이름",
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL, new BigDecimal("545000.00"), new BigDecimal("181000.00")),
                new GeoCoordinate(126.79, 37.50),
                null, null, null, null, null, null, null, null, null, null));

        byte[] csv = """
                기준점번호,종류,기준점명,좌표계구분,X좌표,Y좌표,조사대상여부
                41192D000000001,도근점,1465공,세계,545236.77,181840.96,대상
                """.getBytes(StandardCharsets.UTF_8);

        DuplicateControlPointException thrown = assertThrows(DuplicateControlPointException.class,
                () -> service.importCsv(new ImportSurveyCsvCommand("충돌 조사", STARTED, null, null, csv)));

        assertTrue(thrown.getMessage().contains("41192D000000001"), thrown.getMessage());
    }

    @Test
    @DisplayName("기본 양식에 없어 기준점으로 옮기지 못한 열은 조사 대상에 그대로 보관된다")
    void importCsv_keepsUnrecognizedColumnsOnTarget() throws Exception {
        service.importCsv(new ImportSurveyCsvCommand("2026 일제조사", STARTED, null, null, sampleCsv()));

        ControlPoint row1Point = pointStore.findByPointNo("41192D000001265").orElseThrow();
        SurveyTarget target = targetStore.targets.stream()
                .filter(t -> t.getPointId().equals(row1Point.getId()))
                .findFirst().orElseThrow();

        assertEquals(List.of(new ExtraColumn("순번", "131"), new ExtraColumn("field_20", null)), target.getExtras());
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
            SurveyProject saved = SurveyProject.restore(id, project.getAuthorId(), project.getName(), project.getStartedOn(), project.getEndedOn(), project.getNote());
            projects.put(id, saved);
            return saved;
        }

        @Override
        public SurveyRecord save(SurveyRecord record) {
            long id = record.getId() != null ? record.getId() : ++recordSeq;
            SurveyRecord saved = SurveyRecord.restore(
                    id, record.getProjectId(), record.getPointId(),
                    record.getResult(), record.getSurveyedAt(), record.getNote(), record.getSurveyedById());
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
            SurveyTarget saved = SurveyTarget.restore(
                    ++sequence, target.getProjectId(), target.getPointId(), target.getExtras());
            targets.add(saved);
            return saved;
        }

        @Override
        public List<SurveyTarget> saveAll(List<SurveyTarget> list) {
            return list.stream().map(this::save).toList();
        }
    }
}
