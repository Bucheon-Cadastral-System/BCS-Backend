package com.is.bcs.application.service;

import com.is.bcs.adapter.out.file.SpreadsheetTableExtractor;
import com.is.bcs.application.port.out.file.Table;
import com.is.bcs.application.service.SurveyTargetMapper.ColumnMapping;
import com.is.bcs.application.service.SurveyTargetMapper.MappingResult;
import com.is.bcs.application.service.SurveyTargetMapper.Row;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TraverseInfo;
import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import com.is.bcs.domain.survey.SurveyResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 표 → 도메인 행 매핑 검증 — 열은 위치가 아니라 이름으로 찾는다. */
class SurveyTargetMapperTest {

    private final SpreadsheetTableExtractor extractor = new SpreadsheetTableExtractor();

    private Table tableOf(String resource) throws Exception {
        try (var in = getClass().getResourceAsStream(resource)) {
            return extractor.extract(in.readAllBytes());
        }
    }

    private Table sampleTable() throws Exception {
        return tableOf("/survey-target-sample.csv");
    }

    @Test
    @DisplayName("실파일 49행이 전부 읽히고 첫 행의 모든 값이 원본과 일치한다")
    void map_realFile() throws Exception {
        List<Row> rows = SurveyTargetMapper.map(sampleTable()).rows();

        assertEquals(49, rows.size());
        Row first = rows.getFirst();
        assertEquals("41192D000001265", first.pointNo());
        assertEquals(PointType.DOGEUN, first.type());
        assertEquals("1465공", first.name());
        assertEquals(CoordinateSystem.GRS80_CENTRAL, first.crs());
        assertEquals(0, new BigDecimal("545236.77").compareTo(first.northing())); // X좌표=북방향
        assertEquals(0, new BigDecimal("181840.96").compareTo(first.easting())); // Y좌표=동방향
        assertEquals(126.794623, first.longitude());
        assertEquals(37.506423, first.latitude());
        assertEquals("10300", first.regionCode());
        assertEquals("춘의동", first.regionName());
        assertEquals("경기도 부천시 춘의동 102-16", first.address());
        assertEquals(MarkerMaterial.STONE, first.markerMaterial());
        assertEquals(InstallType.INSTALLED, first.installType());
        assertEquals(LocalDate.of(2018, 2, 21), first.installedDate());
        assertEquals(new TraverseInfo("1", null, null, null), first.traverse());
        assertEquals(SurveyResult.INTACT, first.priorResult());
        assertEquals(LocalDate.of(2025, 9, 8), first.priorSurveyDate());
        assertEquals("대상", first.note());
    }

    @Test
    @DisplayName("어휘 매핑 집계 — 완전 40·망실 3·기타 1·미조사 5, 삼각보조점 1")
    void map_vocabularyCounts() throws Exception {
        List<Row> rows = SurveyTargetMapper.map(sampleTable()).rows();

        assertEquals(40, rows.stream().filter(r -> r.priorResult() == SurveyResult.INTACT).count());
        assertEquals(3, rows.stream().filter(r -> r.priorResult() == SurveyResult.LOST).count());
        assertEquals(1, rows.stream().filter(r -> r.priorResult() == SurveyResult.ETC).count());
        assertEquals(5, rows.stream().filter(r -> r.priorResult() == null).count());
        assertEquals(1, rows.stream().filter(r -> r.type() == PointType.TRIANGULATION_AUX).count());
        assertEquals(48, rows.stream().filter(r -> r.type() == PointType.DOGEUN).count());
    }

    @Test
    @DisplayName("교차구분 라벨 — '도근점'은 일반(false), 빈값은 미기재(null)로 해석한다")
    void map_intersectionLabel() throws Exception {
        List<Row> rows = SurveyTargetMapper.map(sampleTable()).rows();

        assertEquals(42, rows.stream()
                .filter(r -> r.traverse() != null && Boolean.FALSE.equals(r.traverse().intersection()))
                .count());
        assertNull(rows.getFirst().traverse().intersection()); // 행1은 교차구분 빈값
    }

    @Test
    @DisplayName("기본 양식(경위도 열이 없는 20열)도 읽히고 경위도는 비어 있다")
    void map_basicForm() throws Exception {
        List<Row> rows = SurveyTargetMapper.map(tableOf("/survey-target-basic.csv")).rows();

        assertEquals(49, rows.size());
        Row first = rows.getFirst();
        assertNull(first.longitude());
        assertNull(first.latitude());
        assertEquals("41192D000001265", first.pointNo());
        assertEquals(SurveyResult.INTACT, first.priorResult());
    }

    @Test
    @DisplayName("필수 6열만 있어도 읽히고 나머지 항목은 비어 있다")
    void map_minimalColumns() throws Exception {
        List<Row> rows = SurveyTargetMapper.map(tableOf("/survey-target-minimal.csv")).rows();

        assertEquals(3, rows.size());
        Row first = rows.getFirst();
        assertEquals("41192D000001265", first.pointNo());
        assertEquals(PointType.DOGEUN, first.type());
        assertEquals(CoordinateSystem.GRS80_CENTRAL, first.crs());
        assertNull(first.address());
        assertNull(first.markerMaterial());
        assertNull(first.installedDate());
        assertNull(first.traverse());
        assertNull(first.priorResult());
    }

    @Test
    @DisplayName("잘못된 행에서 멈추지 않고 끝까지 훑어 오류를 행 번호와 함께 모은다")
    void map_collectsRowErrors() {
        Table table = new Table(
                List.of("기준점번호", "종류", "기준점명", "좌표계구분", "X좌표", "Y좌표"),
                List.of(
                        List.of("41192D000000001", "도근점", "정상1", "세계", "545000", "181000"),
                        List.of("41192D000000002", "수준점", "이상", "세계", "545000", "181000"),
                        List.of("41192D000000003", "도근점", "정상2", "세계", "545000", "181000"),
                        List.of("41192D000000004", "도근점", "숫자아님", "세계", "좌표", "181000")));

        MappingResult result = SurveyTargetMapper.map(table);

        assertEquals(4, result.totalRows());
        assertEquals(2, result.rows().size()); // 읽힌 행만 남는다
        assertEquals(2, result.errors().size());

        // 첫 오류에서 멈췄다면 뒤쪽 오류(5행)는 나오지 않는다
        assertEquals(3, result.errors().getFirst().row());
        assertTrue(result.errors().getFirst().message().contains("수준점"), result.errors().getFirst().message());
        assertEquals(5, result.errors().getLast().row());
    }

    @Test
    @DisplayName("파일의 열이 어떤 항목으로 읽혔는지와 무시된 열을 함께 알린다")
    void map_reportsColumnMapping() throws Exception {
        ColumnMapping columns = SurveyTargetMapper.map(sampleTable()).columns();

        assertEquals("기준점번호", columns.recognized().get("기준점번호"));
        assertEquals("기존조사내용", columns.recognized().get("기존조사내")); // 잘린 이름 → 표준 항목
        assertEquals("조사대상여부", columns.recognized().get("조사대상여"));
        assertTrue(columns.ignored().contains("순번"), columns.ignored().toString());
        assertTrue(columns.ignored().contains("field_20"), columns.ignored().toString());
    }

    @Test
    @DisplayName("5자에서 잘린 열 이름(기존조사내·조사대상여)도 온전한 이름과 같은 항목으로 읽는다")
    void map_truncatedHeaders() {
        Table table = new Table(
                List.of("기준점번호", "종류", "기준점명", "좌표계구분", "X좌표", "Y좌표", "기존조사내", "조사대상여"),
                List.of(List.of("41192D000001265", "도근점", "1465공", "세계", "545236.77", "181840.96", "망실", "대상")));

        Row row = SurveyTargetMapper.map(table).rows().getFirst();

        assertEquals(SurveyResult.LOST, row.priorResult());
        assertEquals("대상", row.note());
    }

    @Test
    @DisplayName("별칭(관리번호)과 표기 흔들림(띄어쓰기·괄호)을 같은 항목으로 흡수한다")
    void map_aliasesAndSpacing() {
        Table table = new Table(
                List.of("관리번호", "종류", "기준점명", "좌표계 구분", "X 좌표", "Y 좌표", "경도", "위도"),
                List.of(List.of("41192D000001265", "도근점", "1465공", "세계", "545236.77", "181840.96", "126.794623", "37.506423")));

        Row row = SurveyTargetMapper.map(table).rows().getFirst();

        assertEquals("41192D000001265", row.pointNo());
        assertEquals(CoordinateSystem.GRS80_CENTRAL, row.crs());
        assertEquals(0, new BigDecimal("545236.77").compareTo(row.northing()));
        assertEquals(126.794623, row.longitude());
        assertEquals(37.506423, row.latitude());
    }

    @Test
    @DisplayName("열 순서가 달라도 되고 모르는 열은 무시한다")
    void map_ignoresOrderAndUnknownColumns() {
        Table table = new Table(
                List.of("메모", "Y좌표", "종류", "field_20", "기준점명", "X좌표", "좌표계구분", "기준점번호"),
                List.of(List.of("아무거나", "181840.96", "도근점", "", "1465공", "545236.77", "세계", "41192D000001265")));

        Row row = SurveyTargetMapper.map(table).rows().getFirst();

        assertEquals("41192D000001265", row.pointNo());
        assertEquals("1465공", row.name());
        assertEquals(0, new BigDecimal("181840.96").compareTo(row.easting()));
        assertNull(row.address()); // 없는 열은 비어 있을 뿐 거부하지 않는다
    }

    @Test
    @DisplayName("필수 열이 없으면 표준 이름으로 무엇이 빠졌는지 알린다")
    void map_missingRequiredColumns_throws() {
        Table table = new Table(List.of("종류", "기준점명"), List.of());

        InvalidControlPointException thrown = assertThrows(
                InvalidControlPointException.class, () -> SurveyTargetMapper.map(table));

        assertTrue(thrown.getMessage().contains("기준점번호"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("좌표계구분"), thrown.getMessage());
    }
}
