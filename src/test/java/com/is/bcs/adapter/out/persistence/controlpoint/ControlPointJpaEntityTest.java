package com.is.bcs.adapter.out.persistence.controlpoint;

import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpoint.TraverseInfo;
import com.is.bcs.adapter.out.persistence.common.EntityReferenceStubs;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 도메인 ↔ JPA 엔티티 매핑 왕복 검증 — 기대값은 고객사 대상지 CSV 실데이터. */
class ControlPointJpaEntityTest {

    private final EntityManager entityManager = EntityReferenceStubs.entityManager();

    private static ControlPoint csvRow1(Long id) {
        return ControlPoint.restore(
                id, "41192D000001265", PointType.DOGEUN, "1465공",
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                        new BigDecimal("545236.77"), new BigDecimal("181840.96")),
                new GeoCoordinate(126.794623, 37.506423),
                "10300", "춘의동", "경기도 부천시 춘의동 102-16",
                MarkerMaterial.STEEL, InstallType.INSTALLED, LocalDate.of(2018, 2, 21),
                new TraverseInfo("1", null, null, false),
                "망실,안보임", LocalDate.of(2026, 6, 23), 3L);
    }

    @Test
    @DisplayName("도메인→엔티티→도메인 왕복에서 모든 속성이 보존된다")
    void roundTrip_preservesAllAttributes() {
        ControlPoint origin = csvRow1(5L);

        ControlPoint restored = ControlPointJpaEntity.fromDomain(origin, entityManager).toDomain();

        assertEquals(5L, restored.getId());
        assertEquals("41192D000001265", restored.getPointNo());
        assertEquals(PointType.DOGEUN, restored.getType());
        assertEquals("1465공", restored.getName());
        assertEquals(CoordinateSystem.GRS80_CENTRAL, restored.getTm().crs());
        assertEquals(0, new BigDecimal("545236.77").compareTo(restored.getTm().northing()));
        assertEquals(0, new BigDecimal("181840.96").compareTo(restored.getTm().easting()));
        assertEquals(126.794623, restored.getGeo().longitude());
        assertEquals(37.506423, restored.getGeo().latitude());
        assertEquals("10300", restored.getRegionCode());
        assertEquals("춘의동", restored.getRegionName());
        assertEquals("경기도 부천시 춘의동 102-16", restored.getAddress());
        assertEquals(MarkerMaterial.STEEL, restored.getMarkerMaterial());
        assertEquals(InstallType.INSTALLED, restored.getInstallType());
        assertEquals(LocalDate.of(2018, 2, 21), restored.getInstalledDate());
        assertEquals(new TraverseInfo("1", null, null, false), restored.getTraverse());
        assertEquals("망실,안보임", restored.getLastSurveyResult());
        assertEquals(LocalDate.of(2026, 6, 23), restored.getLastSurveyedOn());
        assertEquals(3L, restored.getLastSurveyedById());
    }

    @Test
    @DisplayName("성과 좌표는 소수 4자리 스케일까지 그대로 보존된다")
    void roundTrip_keepsCoordinateScale() {
        ControlPoint origin = ControlPoint.register(
                "41192D000009999", PointType.DOGEUN, "스케일",
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                        new BigDecimal("545236.7712"), new BigDecimal("181840.9605")),
                new GeoCoordinate(126.794623, 37.506423),
                null, null, null, null, null, null, null, null, null, null);

        ControlPoint restored = ControlPointJpaEntity.fromDomain(origin, entityManager).toDomain();

        // compareTo는 스케일을 무시하므로 equals로 자릿수까지 본다
        assertEquals(new BigDecimal("545236.7712"), restored.getTm().northing());
        assertEquals(new BigDecimal("181840.9605"), restored.getTm().easting());
    }

    @Test
    @DisplayName("도선 정보가 없는 점(삼각보조점 등)은 왕복 후에도 traverse가 null이다")
    void roundTrip_withoutTraverse_keepsNull() {
        ControlPoint origin = ControlPoint.restore(
                1L, "41192A000000001", PointType.TRIANGULATION_AUX, "부천25",
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                        new BigDecimal("545000.00"), new BigDecimal("181000.00")),
                new GeoCoordinate(126.79, 37.50),
                null, null, null, null, null, null, null
        , null, null, null);

        ControlPoint restored = ControlPointJpaEntity.fromDomain(origin, entityManager).toDomain();

        assertNull(restored.getTraverse()); // 전 항목 null이면 TraverseInfo(null,null,null,null)이 아니라 null
        assertNull(restored.getRegionCode());
        assertNull(restored.getMarkerMaterial());
        assertNull(restored.getInstallType());
        assertNull(restored.getInstalledDate());
    }
}
