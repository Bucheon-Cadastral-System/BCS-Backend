package com.is.bcs.application.service;

import com.is.bcs.adapter.out.file.SpreadsheetTableExtractor;
import com.is.bcs.adapter.out.geo.Proj4jCoordinateTransformer;
import com.is.bcs.application.port.out.file.Table;
import com.is.bcs.application.service.ImportFileMapper.ColumnMapping;
import com.is.bcs.application.service.ImportFileMapper.MappingResult;
import com.is.bcs.application.service.ImportFileMapper.Row;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TraverseInfo;
import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import com.is.bcs.domain.survey.ExtraColumn;
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
    private final SurveyTargetMapper mapper = new SurveyTargetMapper(new Proj4jCoordinateTransformer());
    // 공통 엔진(열 찾기·어휘·오류 수집)을 확인하는 표는 조사대상여부 열이 없다 — 그 열을 요구하지 않는 서식으로 읽는다
    private final ControlPointFileMapper pointMapper = new ControlPointFileMapper(new Proj4jCoordinateTransformer());

    private Table tableOf(String resource) throws Exception {
        try (var in = getClass().getResourceAsStream(resource)) {
            return extractor.extract(in.readAllBytes());
        }
    }

    private Table sampleTable() throws Exception {
        return tableOf("/survey-target-sample.csv");
    }

    @Test
    @DisplayName("기준점 서식을 대상지로 올리면 막지는 않고 빠진 열로 알린다")
    void map_controlPointFile_warnsWhenReadAsSurveyTarget() throws Exception {
        // 고객사가 준 기준점 파일 — 조사대상여부 대신 최종조사내용·최종조사일자가 있다
        Table table = tableOf("/seed/control-points-bucheon-bessel.xlsx");

        MappingResult asTarget = mapper.map(table);

        // 열이 유동적이라 없다고 거부하면 멀쩡한 대상지 파일까지 막힌다 — 읽되 알린다
        assertTrue(asTarget.rows().size() > 0);
        assertEquals(List.of("조사대상여부"), asTarget.missingColumns());
        // 제 서식으로 읽으면 알릴 것이 없다
        MappingResult asPoints = pointMapper.map(table);
        assertTrue(asPoints.missingColumns().isEmpty());
        assertTrue(asPoints.foreignColumns().isEmpty());
    }

    @Test
    @DisplayName("대상지 서식을 기준점으로 올리면 막지는 않고 다른 서식의 열로 알린다")
    void map_surveyTargetFile_warnsWhenReadAsControlPoints() throws Exception {
        // 기준점 서식에만 있는 열이 없어 빠진 열로는 가려낼 수 없다 — 대상지에만 있는 열이 있는지로 본다
        Table table = sampleTable();

        MappingResult asPoints = pointMapper.map(table);

        assertTrue(asPoints.rows().size() > 0);
        assertEquals(List.of("조사대상여부"), asPoints.foreignColumns());
        // 제 서식으로 읽으면 알릴 것이 없다
        MappingResult asTarget = mapper.map(table);
        assertTrue(asTarget.missingColumns().isEmpty());
        assertTrue(asTarget.foreignColumns().isEmpty());
    }

    @Test
    @DisplayName("실파일 49행이 전부 읽히고 첫 행의 모든 값이 원본과 일치한다")
    void map_realFile() throws Exception {
        List<Row> rows = mapper.map(sampleTable()).rows();

        assertEquals(49, rows.size());
        Row first = rows.getFirst();
        assertEquals("41192D000001265", first.pointNo());
        assertEquals(PointType.DOGEUN, first.type());
        assertEquals("1465공", first.name());
        assertEquals(CoordinateSystem.GRS80_CENTRAL, first.tm().crs());
        assertEquals(0, new BigDecimal("545236.77").compareTo(first.tm().northing())); // X좌표=북방향
        assertEquals(0, new BigDecimal("181840.96").compareTo(first.tm().easting())); // Y좌표=동방향
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
        assertEquals("대상", first.targetMark());
    }

    @Test
    @DisplayName("어휘 매핑 집계 — 완전 40·망실 3·기타 1·미조사 5, 삼각보조점 1")
    void map_vocabularyCounts() throws Exception {
        List<Row> rows = mapper.map(sampleTable()).rows();

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
        List<Row> rows = mapper.map(sampleTable()).rows();

        assertEquals(42, rows.stream()
                .filter(r -> r.traverse() != null && Boolean.FALSE.equals(r.traverse().intersection()))
                .count());
        assertNull(rows.getFirst().traverse().intersection()); // 행1은 교차구분 빈값
    }

    @Test
    @DisplayName("기본 양식(경위도 열이 없는 20열)도 읽히고 경위도는 비어 있다")
    void map_basicForm() throws Exception {
        List<Row> rows = mapper.map(tableOf("/survey-target-basic.csv")).rows();

        assertEquals(49, rows.size());
        Row first = rows.getFirst();
        assertNull(first.longitude());
        assertNull(first.latitude());
        assertEquals("41192D000001265", first.pointNo());
        assertEquals(SurveyResult.INTACT, first.priorResult());
    }

    @Test
    @DisplayName("필수 열만 있고 그 칸이 비어 있어도 읽힌다 — 요구하는 것은 열 이름이지 값이 아니다")
    void map_minimalColumns() throws Exception {
        List<Row> rows = pointMapper.map(tableOf("/survey-target-minimal.csv")).rows();

        assertEquals(3, rows.size());
        Row first = rows.getFirst();
        assertEquals("41192D000001265", first.pointNo());
        assertEquals(PointType.DOGEUN, first.type());
        assertEquals(CoordinateSystem.GRS80_CENTRAL, first.tm().crs());
        assertNull(first.address());
        assertNull(first.markerMaterial());
        assertNull(first.installedDate());
        assertNull(first.traverse());
        assertNull(first.priorResult());
    }

    @Test
    @DisplayName("필수 열이 없으면 행을 읽지 않고 파일째로 거부하며, 빠진 열을 모두 알린다")
    void map_missingRequiredColumns_rejectsFile() {
        // 점을 가리키는 값과 성과만 있는 표 — 옛 양식이거나 내보내기에서 열을 빠뜨린 파일이다
        Table table = new Table(
                List.of("기준점번호", "종류", "기준점명", "좌표계구분", "X좌표", "Y좌표"),
                List.of(List.of("41192D000001265", "도근점", "1465공", "세계", "545236.77", "181840.96")));

        InvalidControlPointException e =
                assertThrows(InvalidControlPointException.class, () -> pointMapper.map(table));

        // 하나만 알리면 고쳐 올린 파일이 다음 열에서 또 걸린다 — 빠진 것을 한 번에 적는다
        assertTrue(e.getMessage().startsWith("필수 열이 없습니다"), e.getMessage());
        for (String column : List.of("토지소재지", "상세주소", "설치일자", "기존조사내용", "기존조사일")) {
            assertTrue(e.getMessage().contains(column), e.getMessage());
        }
    }

    @Test
    @DisplayName("잘못된 행에서 멈추지 않고 끝까지 훑어 오류를 행 번호와 함께 모은다")
    void map_collectsRowErrors() {
        Table table = new Table(
                List.of("기준점번호", "종류", "기준점명", "좌표계구분", "X좌표", "Y좌표", "토지소재지", "상세주소", "설치일자", "기존조사내용", "기존조사일"),
                List.of(
                        List.of("41192D000000001", "도근점", "정상1", "세계", "545000", "181000"),
                        List.of("41192D000000002", "수준점", "이상", "세계", "545000", "181000"),
                        List.of("41192D000000003", "도근점", "정상2", "세계", "545000", "181000"),
                        List.of("41192D000000004", "도근점", "숫자아님", "세계", "좌표", "181000")));

        MappingResult result = pointMapper.map(table);

        assertEquals(4, result.totalRows());
        assertEquals(2, result.rows().size()); // 읽힌 행만 남는다
        assertEquals(2, result.errors().size());

        // 첫 오류에서 멈췄다면 뒤쪽 오류(5행)는 나오지 않는다
        assertEquals(3, result.errors().getFirst().row());
        assertTrue(result.errors().getFirst().message().contains("수준점"), result.errors().getFirst().message());
        assertEquals(5, result.errors().getLast().row());
    }

    @Test
    @DisplayName("파일의 열이 어떤 항목으로 읽혔는지와 값만 보관하는 열을 함께 알린다")
    void map_reportsColumnMapping() throws Exception {
        ColumnMapping columns = mapper.map(sampleTable()).columns();

        assertEquals("기준점번호", columns.recognized().get("기준점번호"));
        assertEquals("기존조사내용", columns.recognized().get("기존조사내")); // 잘린 이름 → 표준 항목
        assertEquals("조사대상여부", columns.recognized().get("조사대상여"));
        assertEquals(List.of("순번", "field_20"), columns.extra()); // 파일에 적힌 순서 그대로
    }

    @Test
    @DisplayName("기본 양식에 없는 열도 이름·값 그대로 남는다 — 빈 칸이어도 열은 사라지지 않는다")
    void map_keepsUnrecognizedColumnValues() throws Exception {
        Row first = mapper.map(sampleTable()).rows().getFirst();

        assertEquals(List.of(new ExtraColumn("순번", "131"), new ExtraColumn("field_20", null)), first.extras());
    }

    @Test
    @DisplayName("고객사가 열을 더해도 코드를 고치지 않고 그 값까지 보관한다")
    void map_columnsAddedByCustomer_areKept() {
        Table table = new Table(
                List.of("기준점번호", "종류", "기준점명", "좌표계구분", "X좌표", "Y좌표", "점검자", "재조사 사유", "토지소재지", "상세주소", "설치일자", "기존조사내용", "기존조사일"),
                List.of(List.of("41192D000001265", "도근점", "1465공", "세계", "545236.77", "181840.96", "김주무관", "표지 훼손")));

        MappingResult result = pointMapper.map(table);

        assertTrue(result.errors().isEmpty());
        assertEquals(List.of("점검자", "재조사 사유"), result.columns().extra());
        assertEquals(
                List.of(new ExtraColumn("점검자", "김주무관"), new ExtraColumn("재조사 사유", "표지 훼손")),
                result.rows().getFirst().extras());
    }

    @Test
    @DisplayName("5자에서 잘린 열 이름(기존조사내·조사대상여)도 온전한 이름과 같은 항목으로 읽는다")
    void map_truncatedHeaders() {
        Table table = new Table(
                List.of("기준점번호", "종류", "기준점명", "좌표계구분", "X좌표", "Y좌표", "기존조사내", "조사대상여", "토지소재지", "상세주소", "설치일자", "기존조사일"),
                List.of(List.of("41192D000001265", "도근점", "1465공", "세계", "545236.77", "181840.96", "망실", "대상")));

        Row row = mapper.map(table).rows().getFirst();

        assertEquals(SurveyResult.LOST, row.priorResult());
        assertEquals("대상", row.targetMark());
    }

    @Test
    @DisplayName("별칭(관리번호)과 표기 흔들림(띄어쓰기·괄호)을 같은 항목으로 흡수한다")
    void map_aliasesAndSpacing() {
        Table table = new Table(
                List.of("관리번호", "종류", "기준점명", "좌표계 구분", "X 좌표", "Y 좌표", "경도", "위도",
                        "토지소재지", "상세주소", "설치일자", "기존조사내용", "기존조사일"),
                List.of(List.of("41192D000001265", "도근점", "1465공", "세계", "545236.77", "181840.96", "126.794623", "37.506423")));

        Row row = pointMapper.map(table).rows().getFirst();

        assertEquals("41192D000001265", row.pointNo());
        assertEquals(CoordinateSystem.GRS80_CENTRAL, row.tm().crs());
        assertEquals(0, new BigDecimal("545236.77").compareTo(row.tm().northing()));
        assertEquals(126.794623, row.longitude());
        assertEquals(37.506423, row.latitude());
    }

    @Test
    @DisplayName("열 순서가 달라도 읽히고, 모르는 열은 제자리 순서대로 보관된다")
    void map_columnOrderDoesNotMatter() {
        Table table = new Table(
                List.of("메모", "Y좌표", "종류", "field_20", "기준점명", "X좌표", "좌표계구분", "기준점번호", "토지소재지", "상세주소", "설치일자", "기존조사내용", "기존조사일"),
                List.of(List.of("아무거나", "181840.96", "도근점", "", "1465공", "545236.77", "세계", "41192D000001265")));

        Row row = pointMapper.map(table).rows().getFirst();

        assertEquals("41192D000001265", row.pointNo());
        assertEquals("1465공", row.name());
        assertEquals(0, new BigDecimal("181840.96").compareTo(row.tm().easting()));
        assertNull(row.address()); // 없는 열은 비어 있을 뿐 거부하지 않는다
        assertEquals(List.of(new ExtraColumn("메모", "아무거나"), new ExtraColumn("field_20", null)), row.extras());
    }

    @Test
    @DisplayName("필수 열의 칸이 비어 있으면 행 오류로 알린다 — 등록 단계까지 미루지 않고 미리보기에서 보이게")
    void map_requiredCellEmpty_isRowError() {
        Table table = new Table(
                List.of("기준점번호", "종류", "기준점명", "좌표계구분", "X좌표", "Y좌표", "토지소재지", "상세주소", "설치일자", "기존조사내용", "기존조사일"),
                List.of(
                        List.of("", "도근점", "1465공", "세계", "545236.77", "181840.96"),
                        List.of("41192D000000002", "도근점", "", "세계", "545000.00", "181000.00")));

        MappingResult result = pointMapper.map(table);

        assertEquals(0, result.rows().size());
        assertEquals(2, result.errors().size());
        assertTrue(result.errors().getFirst().message().contains("기준점번호"), result.errors().getFirst().message());
        assertTrue(result.errors().getLast().message().contains("기준점명"), result.errors().getLast().message());
    }

    @Test
    @DisplayName("같은 기준점이 두 번 나오면 뒤 행을 오류로 알린다 — 뒤 행이 앞 행의 성과를 덮어쓰지 않게")
    void map_duplicatePointInSameFile_isRowError() {
        Table table = new Table(
                List.of("기준점번호", "종류", "기준점명", "좌표계구분", "X좌표", "Y좌표", "토지소재지", "상세주소", "설치일자", "기존조사내용", "기존조사일"),
                List.of(
                        List.of("41192D000000001", "도근점", "1465공", "세계", "545236.77", "181840.96"),
                        List.of("41192D000000002", "도근점", "1465공", "세계", "545000.00", "181000.00")));

        MappingResult result = pointMapper.map(table);

        assertEquals(1, result.rows().size());
        assertEquals(1, result.errors().size());
        assertEquals(3, result.errors().getFirst().row());
        assertTrue(result.errors().getFirst().message().contains("1465공"), result.errors().getFirst().message());
    }

    @Test
    @DisplayName("같은 관리번호가 두 번 나오면 뒤 행을 오류로 알린다 — 기준점 관리번호는 유일하다")
    void map_duplicatePointNoInSameFile_isRowError() {
        Table table = new Table(
                List.of("기준점번호", "종류", "기준점명", "좌표계구분", "X좌표", "Y좌표", "토지소재지", "상세주소", "설치일자", "기존조사내용", "기존조사일"),
                List.of(
                        List.of("41192D000000001", "도근점", "1465공", "세계", "545236.77", "181840.96"),
                        List.of("41192D000000001", "도근점", "1466공", "세계", "545000.00", "181000.00")));

        MappingResult result = pointMapper.map(table);

        assertEquals(1, result.rows().size());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().getFirst().message().contains("41192D000000001"),
                result.errors().getFirst().message());
    }

    @Test
    @DisplayName("모르는 어휘·형식은 어느 항목이 문제인지와 함께 행 오류로 모인다")
    void map_unknownVocabularyAndFormats_areRowErrors() {
        Table table = new Table(
                List.of("기준점번호", "종류", "기준점명", "좌표계구분", "X좌표", "Y좌표",
                        "표지재질", "설치구분", "교차구분", "기존조사내용", "설치일자", "경도(X)", "토지소재지", "상세주소", "기존조사일"),
                List.of(
                        row("1", "도근점", "재질", "세계", "목재", "설치", "도근점", "완전", "2018-02-21", "126.79"),
                        row("2", "도근점", "설치", "세계", "표석", "이설", "도근점", "완전", "2018-02-21", "126.79"),
                        row("3", "도근점", "교차", "세계", "표석", "설치", "삼각점", "완전", "2018-02-21", "126.79"),
                        row("4", "도근점", "결과", "세계", "표석", "설치", "도근점", "반파", "2018-02-21", "126.79"),
                        row("5", "도근점", "날짜", "세계", "표석", "설치", "도근점", "완전", "2018년 2월", "126.79"),
                        row("6", "도근점", "경도", "세계", "표석", "설치", "도근점", "완전", "2018-02-21", "동경"),
                        row("7", "도근점", "좌표계", "동경측지계", "표석", "설치", "도근점", "완전", "2018-02-21", "126.79")));

        MappingResult result = pointMapper.map(table);

        assertEquals(0, result.rows().size());
        assertEquals(7, result.errors().size());
        List<String> messages = result.errors().stream().map(ImportFileMapper.RowError::message).toList();
        assertTrue(messages.get(0).contains("표지재질"), messages.get(0));
        assertTrue(messages.get(1).contains("설치구분"), messages.get(1));
        assertTrue(messages.get(2).contains("교차구분"), messages.get(2));
        assertTrue(messages.get(3).contains("기존조사내용"), messages.get(3));
        assertTrue(messages.get(4).contains("설치일자"), messages.get(4));
        assertTrue(messages.get(5).contains("경도"), messages.get(5));
        assertTrue(messages.get(6).contains("좌표계구분"), messages.get(6));
    }

    @Test
    @DisplayName("관리 지역 밖 좌표는 행을 살려 두고 확인할 자리로만 알린다")
    void map_outsideServiceArea_warnsAndKeepsRow() {
        // 좌표계구분은 '지역'인데 성과는 세계측지계 값이라, 5174로 읽으면 부천에서 100km 북으로 간다
        Table table = new Table(
                List.of("기준점번호", "종류", "기준점명", "좌표계구분", "X좌표", "Y좌표", "토지소재지", "상세주소", "설치일자", "기존조사내용", "기존조사일"),
                List.of(List.of("41192D000010438", "도근점", "5673", "지역", "545163.240", "178356.350")));

        MappingResult result = pointMapper.map(table);

        assertEquals(1, result.rows().size());
        assertTrue(result.errors().isEmpty());
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().getFirst().message().contains("범위 밖"), result.warnings().getFirst().message());
    }

    /** 위 표의 한 행 — 좌표는 어느 행에서도 문제 삼지 않으므로 같은 값을 쓴다. */
    private static List<String> row(String no, String type, String name, String crs,
                                    String material, String install, String intersection,
                                    String priorResult, String installedDate, String longitude) {
        return List.of("41192D00000000" + no, type, name, crs, "545236.77", "181840.96",
                material, install, intersection, priorResult, installedDate, longitude);
    }

    @Test
    @DisplayName("삼각점·삼각보조점 어휘도 읽고, 재설치는 '재설'·'재설치' 둘 다 받는다")
    void map_remainingVocabulary() {
        Table table = new Table(
                List.of("기준점번호", "종류", "기준점명", "좌표계구분", "X좌표", "Y좌표", "설치구분", "토지소재지", "상세주소", "설치일자", "기존조사내용", "기존조사일"),
                List.of(
                        List.of("41192D000000001", "지적삼각점", "삼각1", "세계", "545236.77", "181840.96", "재설"),
                        List.of("41192D000000002", "삼각보조점", "보조1", "세계", "545236.77", "181840.96", "재설치")));

        List<Row> rows = pointMapper.map(table).rows();

        assertEquals(PointType.TRIANGULATION, rows.getFirst().type());
        assertEquals(InstallType.REINSTALLED, rows.getFirst().installType());
        assertEquals(PointType.TRIANGULATION_AUX, rows.getLast().type());
        assertEquals(InstallType.REINSTALLED, rows.getLast().installType());
    }

    @Test
    @DisplayName("이름 없는 열은 건너뛰고, 헤더보다 짧은 행은 뒤쪽 열이 빈 것으로 읽는다")
    void map_blankHeaderAndShortRow() {
        Table table = new Table(
                List.of("기준점번호", "종류", "기준점명", "좌표계구분", "X좌표", "Y좌표", "", "상세주소", "토지소재지", "설치일자", "기존조사내용", "기존조사일"),
                List.of(
                        List.of("41192D000000001", "도근점", "짧은행", "세계", "545236.77", "181840.96"),
                        List.of("41192D000000002", "도근점", "온전한행", "세계", "545236.77", "181840.96", "", "춘의동 1-1")));

        MappingResult result = pointMapper.map(table);

        assertEquals(2, result.rows().size());
        assertNull(result.rows().getFirst().address()); // 행이 짧아 상세주소 칸이 아예 없다
        assertEquals("춘의동 1-1", result.rows().getLast().address());
        assertTrue(result.columns().extra().isEmpty()); // 이름 없는 열은 되살릴 근거가 없어 보관하지 않는다
    }

    @Test
    @DisplayName("필수 열이 없으면 표준 이름으로 무엇이 빠졌는지 알린다")
    void map_missingRequiredColumns_throws() {
        Table table = new Table(List.of("종류", "기준점명"), List.of());

        InvalidControlPointException thrown = assertThrows(
                InvalidControlPointException.class, () -> pointMapper.map(table));

        assertTrue(thrown.getMessage().contains("기준점번호"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("좌표계구분"), thrown.getMessage());
    }
    @Test
    @DisplayName("좌표 변환이 실패한 행은 파일을 멈추지 않고 행 오류가 된다")
    void map_transformFailure_isRowError() {
        // 황당한 성과 값은 투영 라이브러리가 자체 예외를 던질 수 있다 — 파일 문제지 서버 오류가 아니다
        ImportFileMapper failing = new ControlPointFileMapper(tm -> {
            throw new RuntimeException("projection failure");
        });
        Table table = new Table(
                List.of("기준점번호", "종류", "기준점명", "좌표계구분", "X좌표", "Y좌표", "토지소재지", "상세주소", "설치일자", "기존조사내용", "기존조사일"),
                List.of(List.of("41192D000000001", "도근점", "변환불가", "세계", "545236.77", "181840.96")));

        MappingResult result = failing.map(table);

        assertTrue(result.rows().isEmpty());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().getFirst().message().contains("변환할 수 없습니다"),
                result.errors().getFirst().message());
    }

    @Test
    @DisplayName("최종조사내용·최종조사일자 열을 읽는다 — 기준점 마스터의 최근 조사 요약이 된다")
    void map_lastSurveyColumns() {
        Table table = new Table(
                List.of("기준점번호", "종류", "기준점명", "좌표계구분", "X좌표", "Y좌표", "최종조사내용", "최종조사일자", "토지소재지", "상세주소", "설치일자", "기존조사내용", "기존조사일"),
                List.of(List.of("41192D000001265", "도근점", "1465공", "세계", "545236.77", "181840.96", "망실", "20260519")));

        MappingResult result = pointMapper.map(table);

        Row first = result.rows().getFirst();
        assertEquals("망실", first.lastResult());
        assertEquals(LocalDate.of(2026, 5, 19), first.lastSurveyDate());
        assertTrue(result.columns().extra().isEmpty()); // 아는 열이므로 값만 보관하는 열이 아니다
    }
}
