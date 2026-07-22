package com.is.bcs.domain.controlpoint;

import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlPointTest {

    private static final TmCoordinate TM = new TmCoordinate(
            CoordinateSystem.GRS80_CENTRAL, new BigDecimal("545236.77"), new BigDecimal("181840.96"));
    private static final GeoCoordinate GEO = new GeoCoordinate(126.794623, 37.506423);

    private static ControlPoint dogeun() {
        return ControlPoint.register(
                "41192D000001265", PointType.DOGEUN, " 1465공 ", TM, GEO,
                "10300", "춘의동", "경기도 부천시 춘의동 102-16",
                MarkerMaterial.STEEL, InstallType.INSTALLED, LocalDate.of(2018, 2, 21),
                new TraverseInfo("1", "가", "3", false)
        );
    }

    @Test
    @DisplayName("등록 시 관리번호·이름은 트림되고 속성이 보존된다")
    void register_keepsAttributes() {
        ControlPoint point = dogeun();

        assertEquals("41192D000001265", point.getPointNo());
        assertEquals("1465공", point.getName());
        assertEquals(PointType.DOGEUN, point.getType());
        assertEquals(TM, point.getTm());
        assertEquals(GEO, point.getGeo());
        assertEquals("10300", point.getRegionCode());
        assertEquals("춘의동", point.getRegionName());
        assertEquals(MarkerMaterial.STEEL, point.getMarkerMaterial());
        assertEquals(InstallType.INSTALLED, point.getInstallType());
        assertEquals(LocalDate.of(2018, 2, 21), point.getInstalledDate());
        assertEquals(new TraverseInfo("1", "가", "3", false), point.getTraverse());
        assertNull(point.getId());
    }

    @Test
    @DisplayName("도근점 외 속성이 없어도(도선·재질 등) 등록할 수 있다")
    void register_optionalFieldsNullable() {
        ControlPoint point = ControlPoint.register(
                "41192A000000001", PointType.TRIANGULATION_AUX, "부천25", TM, GEO,
                null, null, null, null, null, null, null
        );

        assertNull(point.getTraverse());
        assertNull(point.getMarkerMaterial());
        assertNull(point.getInstalledDate());
    }

    @Test
    @DisplayName("관리번호·이름이 비어 있으면 등록할 수 없다")
    void register_blankIdentity_throws() {
        assertThrows(InvalidControlPointException.class, () -> ControlPoint.register(
                " ", PointType.DOGEUN, "1465공", TM, GEO,
                null, null, null, null, null, null, null));
        assertThrows(InvalidControlPointException.class, () -> ControlPoint.register(
                "41192D000001265", PointType.DOGEUN, " ", TM, GEO,
                null, null, null, null, null, null, null));
    }

    @Test
    @DisplayName("성과 좌표는 좌표계·북방향·동방향이 모두 있어야 한다")
    void tmCoordinate_requiresAllParts() {
        assertThrows(InvalidControlPointException.class,
                () -> new TmCoordinate(null, new BigDecimal("545236.77"), new BigDecimal("181840.96")));
        assertThrows(InvalidControlPointException.class,
                () -> new TmCoordinate(CoordinateSystem.GRS80_CENTRAL, null, new BigDecimal("181840.96")));
        assertThrows(InvalidControlPointException.class,
                () -> new TmCoordinate(CoordinateSystem.GRS80_CENTRAL, new BigDecimal("545236.77"), null));
    }

    @Test
    @DisplayName("경위도 범위를 벗어난 표시 좌표는 만들 수 없다")
    void geoCoordinate_rejectsOutOfRange() {
        assertThrows(InvalidControlPointException.class, () -> new GeoCoordinate(181.0, 37.5));
        assertThrows(InvalidControlPointException.class, () -> new GeoCoordinate(126.79, 95.0));
        assertThrows(InvalidControlPointException.class, () -> new GeoCoordinate(Double.NaN, 37.5));
        assertThrows(InvalidControlPointException.class, () -> new GeoCoordinate(126.79, Double.NaN));
        assertTrue(new GeoCoordinate(126.79, 37.5).longitude() > 0);
    }
}
