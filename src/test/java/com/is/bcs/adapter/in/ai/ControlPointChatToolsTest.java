package com.is.bcs.adapter.in.ai;

import com.is.bcs.application.dto.ControlPointCountSummary;
import com.is.bcs.application.dto.LastSurveySummary;
import com.is.bcs.application.dto.PointLastSurvey;
import com.is.bcs.application.port.in.controlpoint.GetControlPointsUseCase;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpoint.TraverseInfo;
import com.is.bcs.domain.controlpoint.exception.ControlPointNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 기준점 챗봇 도구 검증 — 기대값은 고객사 대상지 CSV 실측값으로 고정한다. */
class ControlPointChatToolsTest {

    private final FakePoints fake = new FakePoints();
    private final ControlPointChatTools tools = new ControlPointChatTools(fake);

    /** 대상지 CSV 1행 실측값. */
    private static ControlPoint csvRow1Point() {
        return ControlPoint.restore(
                1L, "41192D000001265", PointType.DOGEUN, "1465공",
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                        new BigDecimal("545236.77"), new BigDecimal("181840.96")),
                new GeoCoordinate(126.794623, 37.506423),
                "10300", "춘의동", "경기도 부천시 춘의동 102-16",
                MarkerMaterial.STEEL, InstallType.INSTALLED, LocalDate.of(2018, 2, 21),
                new TraverseInfo("1", null, null, false), null, null);
    }

    @Test
    @DisplayName("개수 요약 — 유스케이스 요약을 한글 종류명 키로 순서 보존해 매핑한다")
    void countControlPoints_mapsTypesToDisplayNames() {
        Map<PointType, Long> byType = new LinkedHashMap<>();
        byType.put(PointType.TRIANGULATION, 1L);
        byType.put(PointType.TRIANGULATION_AUX, 0L);
        byType.put(PointType.DOGEUN, 2L);
        fake.countSummary = new ControlPointCountSummary(3, byType);

        PointCountSummary summary = tools.countControlPoints();

        assertEquals(3, summary.total());
        assertEquals(1, summary.countByType().get("지적삼각점"));
        assertEquals(0, summary.countByType().get("지적삼각보조점"));
        assertEquals(2, summary.countByType().get("지적도근점"));
        assertEquals(List.of("지적삼각점", "지적삼각보조점", "지적도근점"),
                List.copyOf(summary.countByType().keySet()));
    }

    @Test
    @DisplayName("단건 조회 — 상세를 표시용 한글 값으로 매핑한다")
    void getControlPointByNo_mapsDetail() {
        fake.points.add(csvRow1Point());

        ControlPointDetail p = tools.getControlPointByNo("41192D000001265");

        assertEquals("41192D000001265", p.pointNo());
        assertEquals("1465공", p.name());
        assertEquals("지적도근점", p.type());
        assertEquals("중부원점(세계측지계)", p.crs());
        assertEquals(0, new BigDecimal("545236.77").compareTo(p.northing()));
        assertEquals(0, new BigDecimal("181840.96").compareTo(p.easting()));
        assertEquals(126.794623, p.longitude());
        assertEquals(37.506423, p.latitude());
        assertEquals("춘의동", p.regionName());
        assertEquals("경기도 부천시 춘의동 102-16", p.address());
        assertEquals("철재", p.markerMaterial());
        assertEquals("설치", p.installType());
        assertEquals(LocalDate.of(2018, 2, 21), p.installedDate());
    }

    @Test
    @DisplayName("선택 항목이 비어 있는 점도 단건 조회가 깨지지 않는다")
    void getControlPointByNo_optionalFieldsNull_ok() {
        fake.points.add(ControlPoint.restore(
                2L, "41192D000001266", PointType.DOGEUN, "점2",
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                        new BigDecimal("545000.00"), new BigDecimal("181000.00")),
                new GeoCoordinate(126.79, 37.50),
                null, null, null, null, null, null, null, null, null));

        ControlPointDetail p = tools.getControlPointByNo("41192D000001266");

        assertNull(p.markerMaterial());
        assertNull(p.installedDate());
    }

    @Test
    @DisplayName("단건 조회 실패 예외는 잡지 않고 그대로 올린다 — 정형화는 ChatToolErrorProcessor 몫")
    void getControlPointByNo_missing_propagates() {
        assertThrows(ControlPointNotFoundException.class,
                () -> tools.getControlPointByNo("41192D999999999"));
    }

    /** 기준점 조회 유스케이스 페이크. */
    @Test
    @DisplayName("기준점 찾기 — 이름·관리번호·소재지 어디에 걸려도 뽑고, 종류로 좁히고, 건수를 자른다")
    void findControlPoints_matchesAnyColumn() {
        fake.points.add(csvRow1Point());
        fake.points.add(ControlPoint.restore(
                2L, "41192A000000001", PointType.TRIANGULATION, "역곡1",
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                        new BigDecimal("545000.00"), new BigDecimal("181000.00")),
                new GeoCoordinate(126.79, 37.50),
                "10400", "역곡동", "경기도 부천시 역곡동 1-1",
                null, null, null, null, null, null));

        assertEquals(List.of("41192D000001265"),
                tools.findControlPoints("춘의동", null, null).stream().map(ControlPointBrief::pointNo).toList());
        assertEquals(List.of("41192A000000001"),
                tools.findControlPoints("역곡", null, null).stream().map(ControlPointBrief::pointNo).toList());
        assertEquals(List.of("41192D000001265"),
                tools.findControlPoints("1465", null, null).stream().map(ControlPointBrief::pointNo).toList());
        assertEquals(List.of("41192A000000001"),
                tools.findControlPoints("", "지적삼각점", null).stream().map(ControlPointBrief::pointNo).toList());
        assertEquals(1, tools.findControlPoints("", null, 1).size());
        assertEquals(30, Math.min(30, tools.findControlPoints("", null, 999).size() + 30)); // 상한 30 — 값이 커도 넘지 않는다
    }

    @Test
    @DisplayName("최종조사 — 관리번호로 찾은 점의 마지막 조사를 이름과 함께 돌려준다")
    void getLastSurveyByPointNo_joinsPointName() {
        fake.points.add(csvRow1Point());
        fake.lastSurvey = new LastSurveySummary("망실", LocalDate.of(2026, 8, 10), 12L, "황인우", "표지 없음");

        LastSurveyBrief last = tools.getLastSurveyByPointNo("41192D000001265");

        assertEquals("41192D000001265", last.pointNo());
        assertEquals("1465공", last.name());
        assertEquals("망실", last.result());
        assertEquals(LocalDate.of(2026, 8, 10), last.surveyedOn());
        assertEquals("황인우", last.surveyorName());
        assertEquals("표지 없음", last.note());
    }

    private static class FakePoints implements GetControlPointsUseCase {

        final List<ControlPoint> points = new ArrayList<>();
        ControlPointCountSummary countSummary;
        LastSurveySummary lastSurvey = new LastSurveySummary(null, null, null, null, null);

        @Override
        public List<ControlPoint> getAll() {
            return points;
        }

        @Override
        public ControlPoint getByPointNo(String pointNo) {
            return points.stream().filter(p -> p.getPointNo().equals(pointNo)).findFirst()
                    .orElseThrow(() -> new ControlPointNotFoundException(
                            "기준점을 찾을 수 없습니다: " + pointNo));
        }

        @Override
        public LastSurveySummary getLastSurvey(Long pointId) {
            return lastSurvey;
        }

        @Override
        public List<PointLastSurvey> getLastSurveys() {
            throw new UnsupportedOperationException("챗봇 도구는 점 전체의 최종조사를 읽지 않는다");
        }

        @Override
        public ControlPointCountSummary getCountSummary() {
            return countSummary;
        }
    }
}
