package com.is.bcs.adapter.in.bootstrap;

import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.PointType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DogeunSeedCsvTest {

    @Test
    @DisplayName("시드 파일 2,146점이 전부 도근점·동경측지계(5174) 성과로 읽힌다")
    void load_allRows() {
        List<ControlPoint> points = DogeunSeedCsv.load();

        assertEquals(2146, points.size());
        assertTrue(points.stream().allMatch(p -> p.getType() == PointType.DOGEUN));
        assertTrue(points.stream().allMatch(p -> p.getTm().crs() == CoordinateSystem.BESSEL_CENTRAL));
        assertEquals(2146, points.stream().map(ControlPoint::getPointNo).distinct().count());
    }

    @Test
    @DisplayName("첫 행의 관리번호·이름·좌표가 원본 값과 일치한다")
    void load_firstRow() {
        ControlPoint first = DogeunSeedCsv.load().get(0);

        assertEquals("41192D000001065", first.getPointNo());
        assertEquals("1254", first.getName());
        assertEquals(126.7939202, first.getGeo().longitude());
        assertEquals(37.494528, first.getGeo().latitude());
        assertEquals(0, new BigDecimal("181706.08").compareTo(first.getTm().easting()));
        assertEquals(0, new BigDecimal("443611.77").compareTo(first.getTm().northing()));
    }
}
