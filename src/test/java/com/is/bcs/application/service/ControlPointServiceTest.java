package com.is.bcs.application.service;

import com.is.bcs.adapter.out.geo.Proj4jCoordinateTransformer;
import com.is.bcs.application.dto.ControlPointCountSummary;
import com.is.bcs.application.dto.LastSurveySummary;
import com.is.bcs.application.dto.RegisterControlPointCommand;
import com.is.bcs.application.dto.RegisterControlPointResult;
import com.is.bcs.application.dto.UpdateControlPointCommand;
import com.is.bcs.application.dto.UpdateControlPointResult;
import com.is.bcs.application.port.out.survey.LoadSurveyRecordPort;
import com.is.bcs.application.port.out.survey.LoadSurveyTargetPort;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpoint.TraverseInfo;
import com.is.bcs.domain.controlpoint.exception.ControlPointInUseException;
import com.is.bcs.domain.controlpoint.exception.ControlPointModifiedException;
import com.is.bcs.domain.controlpoint.exception.ControlPointNotFoundException;
import com.is.bcs.domain.controlpoint.exception.DuplicateControlPointException;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import com.is.bcs.support.FakeControlPointStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.is.bcs.config.TimeConfig;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 수동 한 점 등록 — 파일 임포트와 같은 규칙(이름·종류 매칭, 있으면 갱신)을 따르는지 검증한다. */
class ControlPointServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-22T09:00:00Z"), TimeConfig.KST);

    private final FakeControlPointStore store = new FakeControlPointStore();
    private final FakeSurveyUsage surveyUsage = new FakeSurveyUsage();
    private final ControlPointService service = new ControlPointService(
            store, store, store, new ControlPointRegistrar(store, store), new Proj4jCoordinateTransformer(),
            surveyUsage, surveyUsage, ids -> Map.of(), FIXED_CLOCK);

    private static RegisterControlPointCommand csvRow1Command() {
        return new RegisterControlPointCommand(
                "41192D000001265", PointType.DOGEUN, "1465공",
                CoordinateSystem.GRS80_CENTRAL,
                new BigDecimal("545236.77"), new BigDecimal("181840.96"),
                "10300", "춘의동", "경기도 부천시 춘의동 102-16",
                MarkerMaterial.STEEL, InstallType.INSTALLED, LocalDate.of(2018, 2, 21),
                new TraverseInfo("1", null, null, false)
        );
    }

    /** 이름 표를 든 서비스 — 기본 서비스는 빈 표라 조사원 이름이 붙는지 볼 수 없다. */
    private ControlPointService serviceWithNames(Map<Long, String> names) {
        return new ControlPointService(
                store, store, store, new ControlPointRegistrar(store, store), new Proj4jCoordinateTransformer(),
                surveyUsage, surveyUsage, ids -> names, FIXED_CLOCK);
    }

    /** 파일이 적어 온 최종조사를 든 점 — 이 세 칸은 임포트만 쓴다. */
    private ControlPoint pointWithFileSurvey(String result, LocalDate surveyedOn) {
        ControlPoint registered = service.register(csvRow1Command()).point();
        return store.save(ControlPoint.restore(
                registered.getId(), registered.getPointNo(), registered.getType(), registered.getName(),
                registered.getTm(), registered.getGeo(), registered.getRegionCode(), registered.getRegionName(),
                registered.getAddress(), registered.getMarkerMaterial(), registered.getInstallType(),
                registered.getInstalledDate(), registered.getTraverse(), result, surveyedOn));
    }

    @Test
    @DisplayName("앱이 남긴 기록이 있으면 그 판정을 따르고 조사원 이름을 붙인다")
    void getLastSurvey_prefersRecordOverFileValue() {
        ControlPoint point = pointWithFileSurvey("정상", LocalDate.of(2025, 9, 8));
        surveyUsage.records.add(SurveyRecord.restore(
                1L, point.getId(), SurveyResult.LOST, OffsetDateTime.parse("2026-07-22T10:00:00+09:00"), null, 7L));

        LastSurveySummary summary = serviceWithNames(Map.of(7L, "김측량")).getLastSurvey(point.getId());

        assertEquals("망실", summary.result());
        assertEquals(LocalDate.of(2026, 7, 22), summary.surveyedOn());
        assertEquals("김측량", summary.surveyorName());
    }

    @Test
    @DisplayName("기록이 없으면 시드 조사를 보인다 — 올라오기 전까지의 총정리가 유일한 정보다")
    void getLastSurvey_withoutRecord_fallsBackToFileValue() {
        ControlPoint point = pointWithFileSurvey("정상", LocalDate.of(2025, 9, 8));

        LastSurveySummary summary = service.getLastSurvey(point.getId());

        assertEquals("정상", summary.result());
        assertEquals(LocalDate.of(2025, 9, 8), summary.surveyedOn());
        assertNull(summary.surveyorName()); // 파일에는 조사원이 없다
    }

    @Test
    @DisplayName("여러 회차의 기록 중 조사 시각이 가장 늦은 판정을 따른다")
    void getLastSurvey_takesLatestRecordAcrossProjects() {
        ControlPoint point = pointWithFileSurvey("정상", LocalDate.of(2025, 9, 8));
        surveyUsage.records.add(SurveyRecord.restore(
                1L, point.getId(), SurveyResult.LOST, OffsetDateTime.parse("2026-07-22T10:00:00+09:00"), null, null));
        surveyUsage.records.add(SurveyRecord.restore(
                2L, point.getId(), SurveyResult.UNAVAILABLE, OffsetDateTime.parse("2026-08-01T10:00:00+09:00"), null, null));

        assertEquals("조사불가", service.getLastSurvey(point.getId()).result());
    }

    @Test
    @DisplayName("조사 시각이 같으면 나중에 남긴 기록의 판정을 따른다 — 옛 회차에 붙은 기록이라도")
    void getLastSurvey_sameInstant_prefersLaterRecord() {
        ControlPoint point = pointWithFileSurvey("정상", LocalDate.of(2025, 9, 8));
        OffsetDateTime same = OffsetDateTime.parse("2026-07-22T10:00:00+09:00");
        // 파일로 들어온 기록은 조사일의 자정을 시각으로 쓰므로 서로 다른 회차가 같은 날짜를 적으면 시각이 완전히 겹친다.
        // 나중에 담은 쪽이 더 앞선 회차(1번)라 프로젝트 번호로 가르면 순서가 뒤집힌다
        surveyUsage.records.add(SurveyRecord.restore(2L, point.getId(), SurveyResult.INTACT, same, null, null));
        surveyUsage.records.add(SurveyRecord.restore(1L, point.getId(), SurveyResult.LOST, same, null, null));

        assertEquals("망실", service.getLastSurvey(point.getId()).result());
    }

    @Test
    @DisplayName("최종조사일은 KST 날짜다 — 같은 순간이라도 지역에 따라 날짜가 갈린다")
    void getLastSurvey_derivesDateInKst() {
        ControlPoint point = pointWithFileSurvey(null, null);
        // UTC 20시는 KST 로 다음 날 05시다. 날짜를 UTC 로 뽑으면 하루 앞선 날이 적힌다
        surveyUsage.records.add(SurveyRecord.restore(
                1L, point.getId(), SurveyResult.INTACT, OffsetDateTime.parse("2026-07-22T20:00:00Z"), null, null));

        assertEquals(LocalDate.of(2026, 7, 23), service.getLastSurvey(point.getId()).surveyedOn());
    }

    @Test
    @DisplayName("시드보다 오래된 기록이 있으면 시드를 따른다 — 임포트가 기존조사일로 과거 기록을 만든다")
    void getLastSurvey_seedNewerThanRecord_prefersSeed() {
        ControlPoint point = pointWithFileSurvey("망실", LocalDate.of(2026, 6, 23));
        surveyUsage.records.add(SurveyRecord.restore(
                1L, point.getId(), SurveyResult.INTACT, OffsetDateTime.parse("2025-09-08T10:00:00+09:00"), null, null));

        LastSurveySummary summary = service.getLastSurvey(point.getId());

        assertEquals("망실", summary.result());
        assertEquals(LocalDate.of(2026, 6, 23), summary.surveyedOn());
    }

    @Test
    @DisplayName("수정 창을 열어 둔 사이 다른 사람이 먼저 고쳤으면 덮지 않고 거절한다")
    void update_staleVersion_rejected() {
        ControlPoint saved = store.save(ControlPoint.restore(
                1L, "41192D000001265", PointType.DOGEUN, "1465공",
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                        new BigDecimal("545236.77"), new BigDecimal("181840.96")),
                new GeoCoordinate(126.794623, 37.506423),
                null, null, null, null, null, null, null, null, null, 3L));

        // 화면이 2판을 보고 있는 사이 저장된 것은 3판이다
        assertThrows(ControlPointModifiedException.class, () -> service.update(new UpdateControlPointCommand(
                saved.getId(), "41192D000009999", PointType.DOGEUN, "1465공",
                CoordinateSystem.GRS80_CENTRAL,
                new BigDecimal("545240.00"), new BigDecimal("181845.00"), 2L)));

        assertEquals("41192D000001265", store.findById(saved.getId()).orElseThrow().getPointNo());
    }

    @Test
    @DisplayName("기타 판정이면 비고도 함께 실린다 — 결과만으로는 무엇이었는지 알 수 없다")
    void getLastSurvey_etc_carriesNote() {
        ControlPoint point = pointWithFileSurvey(null, null);
        surveyUsage.records.add(SurveyRecord.restore(
                1L, point.getId(), SurveyResult.ETC, OffsetDateTime.parse("2026-07-22T10:00:00+09:00"),
                "포장 공사로 덮여 있음", null));

        LastSurveySummary summary = service.getLastSurvey(point.getId());

        assertEquals("기타", summary.result());
        assertEquals("포장 공사로 덮여 있음", summary.note());
    }

    @Test
    @DisplayName("파일 값도 기록도 없으면 세 칸이 모두 비어 있다")
    void getLastSurvey_neverSurveyed_isEmpty() {
        Long id = service.register(csvRow1Command()).point().getId();

        LastSurveySummary summary = service.getLastSurvey(id);

        assertNull(summary.result());
        assertNull(summary.surveyedOn());
        assertNull(summary.surveyorName());
    }

    @Test
    @DisplayName("없는 점의 최종조사를 물으면 찾을 수 없다고 답한다")
    void getLastSurvey_unknownPoint_throws() {
        assertThrows(ControlPointNotFoundException.class, () -> service.getLastSurvey(9999L));
    }

    @Test
    @DisplayName("등록하면 id가 발급되고, 경위도는 성과(TM)에서 서버가 파생한다")
    void register_savesPointAndDerivesGeo() {
        RegisterControlPointResult result = service.register(csvRow1Command());

        assertTrue(result.created());
        assertNotNull(result.point().getId());
        assertEquals("41192D000001265", result.point().getPointNo());
        assertEquals(0, new BigDecimal("545236.77").compareTo(result.point().getTm().northing()));
        // 정답지 = 굴착협의 CSV 같은 행의 경위도(실측 편차 약 4cm)
        assertEquals(126.794623, result.point().getGeo().longitude(), 1e-6);
        assertEquals(37.506423, result.point().getGeo().latitude(), 1e-6);
        assertNull(result.warning());
        assertEquals(1, store.findAll().size());
    }

    @Test
    @DisplayName("같은 이름·종류의 점이 있으면 새 점을 만들지 않고 그 점을 입력 값으로 갱신한다 — 임포트와 같은 규칙")
    void register_sameNameAndType_updatesExistingPoint() {
        Long id = service.register(csvRow1Command()).point().getId();

        RegisterControlPointCommand moved = new RegisterControlPointCommand(
                "41192D000001265", PointType.DOGEUN, "1465공",
                CoordinateSystem.GRS80_CENTRAL,
                new BigDecimal("545240.00"), new BigDecimal("181845.00"),
                null, null, null, null, null, null, null
        );
        RegisterControlPointResult result = service.register(moved);

        assertFalse(result.created());
        assertTrue(result.updated());
        assertEquals(id, result.point().getId()); // 갱신은 id를 보존한다
        assertEquals(0, new BigDecimal("545240.00").compareTo(result.point().getTm().northing()));
        assertEquals(1, store.findAll().size());
    }

    @Test
    @DisplayName("입력 칸에 없는 선택 항목은 등록이 지우지 않는다 — 성과까지 같으면 갱신도 아니다")
    void register_withoutOptionalFields_keepsExistingValues() {
        service.register(csvRow1Command());

        RegisterControlPointCommand bare = new RegisterControlPointCommand(
                "41192D000001265", PointType.DOGEUN, "1465공",
                CoordinateSystem.GRS80_CENTRAL,
                new BigDecimal("545236.77"), new BigDecimal("181840.96"),
                null, null, null, null, null, null, null
        );
        RegisterControlPointResult result = service.register(bare);

        assertFalse(result.created());
        assertFalse(result.updated());
        assertEquals("경기도 부천시 춘의동 102-16", result.point().getAddress());
        assertEquals(MarkerMaterial.STEEL, result.point().getMarkerMaterial());
        assertEquals(LocalDate.of(2018, 2, 21), result.point().getInstalledDate());
    }

    @Test
    @DisplayName("값까지 같은 점을 다시 등록하면 아무것도 바꾸지 않고 그 점을 돌려준다")
    void register_identicalPoint_reusesWithoutChange() {
        service.register(csvRow1Command());

        RegisterControlPointResult result = service.register(csvRow1Command());

        assertFalse(result.created());
        assertFalse(result.updated());
        assertEquals(1, store.findAll().size());
    }

    @Test
    @DisplayName("다른 이름의 점이 쓰는 관리번호로는 등록할 수 없다")
    void register_pointNoTakenByOtherPoint_throws() {
        service.register(csvRow1Command());

        RegisterControlPointCommand other = new RegisterControlPointCommand(
                "41192D000001265", PointType.DOGEUN, "9999공",
                CoordinateSystem.GRS80_CENTRAL,
                new BigDecimal("545000.00"), new BigDecimal("181000.00"),
                null, null, null, null, null, null, null
        );

        assertThrows(DuplicateControlPointException.class, () -> service.register(other));
        assertEquals(1, store.findAll().size()); // 실패한 등록은 저장되지 않는다
    }

    @Test
    @DisplayName("공백이 섞인 관리번호는 정규화되어 같은 점으로 맞춰진다")
    void register_whitespacePointNo_matchesSamePoint() {
        service.register(csvRow1Command());

        RegisterControlPointCommand padded = new RegisterControlPointCommand(
                "  41192D000001265  ", PointType.DOGEUN, "1465공",
                CoordinateSystem.GRS80_CENTRAL,
                new BigDecimal("545236.77"), new BigDecimal("181840.96"),
                "10300", "춘의동", "경기도 부천시 춘의동 102-16",
                MarkerMaterial.STEEL, InstallType.INSTALLED, LocalDate.of(2018, 2, 21),
                new TraverseInfo("1", null, null, false)
        );
        RegisterControlPointResult result = service.register(padded);

        assertFalse(result.created());
        assertFalse(result.updated());
        assertEquals(1, store.findAll().size());
    }

    @Test
    @DisplayName("수정 — 식별·성과가 바뀌고 경위도는 재파생되며, 화면에 없는 선택 항목은 그대로다")
    void update_changesIdentityAndKeepsOptionalFields() {
        Long id = service.register(csvRow1Command()).point().getId();

        UpdateControlPointResult result = service.update(new UpdateControlPointCommand(
                id, " 41192D000012345 ", PointType.DOGEUN, " 1465공(이설) ",
                CoordinateSystem.GRS80_CENTRAL,
                new BigDecimal("545240.00"), new BigDecimal("181845.00"), 0L));

        assertEquals(id, result.point().getId());
        assertEquals("41192D000012345", result.point().getPointNo()); // 다듬어(trim) 저장
        assertEquals("1465공(이설)", result.point().getName());
        assertEquals(0, new BigDecimal("545240.00").compareTo(result.point().getTm().northing()));
        assertEquals(37.506423, result.point().getGeo().latitude(), 1e-4); // 성과에서 다시 파생
        assertEquals("경기도 부천시 춘의동 102-16", result.point().getAddress()); // 요청에 없는 항목은 유지
        assertEquals(MarkerMaterial.STEEL, result.point().getMarkerMaterial());
        assertNull(result.warning());
        assertEquals(1, store.findAll().size());
    }

    @Test
    @DisplayName("수정 — 자기 자신의 관리번호·이름은 충돌이 아니고, 없는 점 수정은 거부한다")
    void update_selfIdentityAllowed_missingPointRejected() {
        Long id = service.register(csvRow1Command()).point().getId();

        UpdateControlPointResult result = service.update(new UpdateControlPointCommand(
                id, "41192D000001265", PointType.DOGEUN, "1465공",
                CoordinateSystem.GRS80_CENTRAL,
                new BigDecimal("545300.00"), new BigDecimal("181900.00"), 0L));

        assertEquals(id, result.point().getId());
        assertThrows(ControlPointNotFoundException.class, () -> service.update(new UpdateControlPointCommand(
                999L, "41192D000000001", PointType.DOGEUN, "이름",
                CoordinateSystem.GRS80_CENTRAL, new BigDecimal("545000.00"), new BigDecimal("181000.00"), 0L)));
    }

    @Test
    @DisplayName("수정 — 다른 점의 관리번호나 이름·종류로는 바꿀 수 없다(임포트 매칭 키 보호)")
    void update_conflictsWithOtherPoint_throws() {
        service.register(csvRow1Command());
        RegisterControlPointCommand other = new RegisterControlPointCommand(
                "41192D000009999", PointType.DOGEUN, "9999공",
                CoordinateSystem.GRS80_CENTRAL,
                new BigDecimal("545100.00"), new BigDecimal("181100.00"),
                null, null, null, null, null, null, null);
        Long otherId = service.register(other).point().getId();

        assertThrows(DuplicateControlPointException.class, () -> service.update(new UpdateControlPointCommand(
                otherId, "41192D000001265", PointType.DOGEUN, "9999공",
                CoordinateSystem.GRS80_CENTRAL, new BigDecimal("545100.00"), new BigDecimal("181100.00"), 0L)));
        assertThrows(DuplicateControlPointException.class, () -> service.update(new UpdateControlPointCommand(
                otherId, "41192D000009999", PointType.DOGEUN, "1465공",
                CoordinateSystem.GRS80_CENTRAL, new BigDecimal("545100.00"), new BigDecimal("181100.00"), 0L)));
        assertEquals("9999공", service.getByPointNo("41192D000009999").getName()); // 거부된 수정은 남지 않는다
    }

    @Test
    @DisplayName("삭제 — 점이 지워지고, 없는 점 삭제는 거부한다")
    void delete_removesPoint() {
        Long id = service.register(csvRow1Command()).point().getId();

        service.delete(id);

        assertEquals(0, store.findAll().size());
        assertThrows(ControlPointNotFoundException.class, () -> service.delete(id));
    }

    @Test
    @DisplayName("삭제 — 조사 프로젝트가 대상이나 기록으로 쓰는 점은 지울 수 없다(조사 데이터는 프로젝트 소유)")
    void delete_referencedBySurvey_rejected() {
        Long id = service.register(csvRow1Command()).point().getId();

        surveyUsage.targetUsed = true;
        assertThrows(ControlPointInUseException.class, () -> service.delete(id));

        surveyUsage.targetUsed = false;
        surveyUsage.recordUsed = true;
        assertThrows(ControlPointInUseException.class, () -> service.delete(id));

        assertEquals(1, store.findAll().size()); // 거부된 삭제는 점을 남긴다
    }

    @Test
    @DisplayName("참조 여부 — 대상이나 기록 어느 쪽이 참조해도 참, 없는 점은 거부한다")
    void isReferenced_reflectsSurveyUsage() {
        Long id = service.register(csvRow1Command()).point().getId();

        assertFalse(service.isReferenced(id));
        surveyUsage.targetUsed = true;
        assertTrue(service.isReferenced(id));
        surveyUsage.targetUsed = false;
        surveyUsage.recordUsed = true;
        assertTrue(service.isReferenced(id));
        assertThrows(ControlPointNotFoundException.class, () -> service.isReferenced(999L));
    }

    @Test
    @DisplayName("부천 범위 밖 좌표도 등록은 되고, 확인하라는 경고가 함께 온다")
    void register_outsideServiceArea_registersWithWarning() {
        // 위도가 부천 남쪽으로 크게 벗어나는 성과 — 좌표계를 잘못 고른 상황과 같은 모양
        RegisterControlPointCommand far = new RegisterControlPointCommand(
                "41192D000009999", PointType.DOGEUN, "멀리",
                CoordinateSystem.GRS80_CENTRAL,
                new BigDecimal("445000.00"), new BigDecimal("181000.00"),
                null, null, null, null, null, null, null
        );

        RegisterControlPointResult result = service.register(far);

        assertTrue(result.created());
        assertNotNull(result.warning());
        assertTrue(result.warning().contains("부천시"), result.warning());
        assertEquals(1, store.findAll().size());
    }

    @Test
    @DisplayName("관리번호로 조회하고, 없으면 ControlPointNotFoundException")
    void getByPointNo() {
        service.register(csvRow1Command());

        assertEquals("1465공", service.getByPointNo("41192D000001265").getName());
        assertThrows(ControlPointNotFoundException.class, () -> service.getByPointNo("41192D999999999"));
    }

    @Test
    @DisplayName("전체 조회는 저장된 기준점 전부를 반환한다")
    void getAll_returnsAllSaved() {
        service.register(csvRow1Command());

        assertEquals(1, service.getAll().size());
    }

    @Test
    @DisplayName("개수 요약 — 전체 개수와 종류별 개수를 종류 순서대로, 없는 종류도 0으로 채워 준다")
    void getCountSummary_returnsZeroFilledCountsInTypeOrder() {
        service.register(csvRow1Command());
        service.register(minimalCommand("41192D000001266", PointType.DOGEUN));
        service.register(minimalCommand("41190A000000001", PointType.TRIANGULATION));

        ControlPointCountSummary summary = service.getCountSummary();

        assertEquals(3, summary.total());
        assertEquals(1, summary.countByType().get(PointType.TRIANGULATION));
        assertEquals(0, summary.countByType().get(PointType.TRIANGULATION_AUX));
        assertEquals(2, summary.countByType().get(PointType.DOGEUN));
        assertEquals(List.of(PointType.values()), List.copyOf(summary.countByType().keySet()));
    }

    private static RegisterControlPointCommand minimalCommand(String pointNo, PointType type) {
        return new RegisterControlPointCommand(
                pointNo, type, "점-" + pointNo,
                CoordinateSystem.GRS80_CENTRAL,
                new BigDecimal("545000.00"), new BigDecimal("181000.00"),
                null, null, null, null, null, null, null
        );
    }

    /** 점 삭제 가부 판정용 페이크 — 대상·기록 참조 여부만 흉내 내고 나머지 조회는 쓰지 않는다. */
    private static class FakeSurveyUsage implements LoadSurveyTargetPort, LoadSurveyRecordPort {

        boolean targetUsed = false;
        boolean recordUsed = false;
        final List<SurveyRecord> records = new ArrayList<>();

        @Override
        public boolean existsByPointId(Long pointId) {
            return targetUsed;
        }

        @Override
        public boolean lockByProjectIdAndPointId(Long projectId, Long pointId) {
            throw new UnsupportedOperationException("기준점 서비스는 대상 잠금을 쓰지 않는다");
        }

        @Override
        public boolean existsRecordByPointId(Long pointId) {
            return recordUsed;
        }

        @Override
        public long countByProjectId(Long projectId) {
            return 0;
        }

        @Override
        public java.util.Map<Long, Long> countTargetsByProject() {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map<Long, Long> countSurveyedByProject() {
            return java.util.Map.of();
        }

        @Override
        public List<Long> findPointIdsByProjectId(Long projectId) {
            return List.of();
        }

        @Override
        public List<SurveyRecord> findRecordsByProjectId(Long projectId) {
            return List.of();
        }

        @Override

        public List<SurveyRecord> findRecordsByPointId(Long pointId) {

            return records.stream().filter(r -> r.getPointId().equals(pointId)).toList();

        }

        /** 조사 시각이 겹치면 나중에 담은 기록이 이긴다 — 실제 어댑터가 기록을 만든 시각으로 가르는 것과 같은 규칙이다. */
        @Override
        public Optional<SurveyRecord> findLatestRecordByPointId(Long pointId) {
            return findRecordsByPointId(pointId).stream()
                    .reduce((older, newer) -> newer.getSurveyedAt().isBefore(older.getSurveyedAt()) ? older : newer);
        }

        @Override
        public List<com.is.bcs.application.dto.SurveyRecordSummary> findRecordSummariesByProjectId(Long projectId) {
            return List.of();
        }

        @Override
        public Optional<SurveyRecord> findRecordByProjectIdAndPointId(Long projectId, Long pointId) {
            return Optional.empty();
        }

        @Override
        public Map<SurveyResult, Long> countByResult(Long projectId) {
            return Map.of();
        }
    }
}
