package com.is.bcs.application.service;

import com.is.bcs.adapter.out.geo.Proj4jCoordinateTransformer;
import com.is.bcs.application.dto.ControlPointCountSummary;
import com.is.bcs.application.dto.RegisterControlPointCommand;
import com.is.bcs.application.dto.RegisterControlPointResult;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TraverseInfo;
import com.is.bcs.domain.controlpoint.exception.ControlPointNotFoundException;
import com.is.bcs.domain.controlpoint.exception.DuplicateControlPointException;
import com.is.bcs.support.FakeControlPointStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 수동 한 점 등록 — 파일 임포트와 같은 규칙(이름·종류 매칭, 있으면 갱신)을 따르는지 검증한다. */
class ControlPointServiceTest {

    private final FakeControlPointStore store = new FakeControlPointStore();
    private final ControlPointService service = new ControlPointService(
            store, new ControlPointRegistrar(store, store), new Proj4jCoordinateTransformer());

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
}
