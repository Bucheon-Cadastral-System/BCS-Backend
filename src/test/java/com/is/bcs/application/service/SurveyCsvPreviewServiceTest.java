package com.is.bcs.application.service;

import com.is.bcs.adapter.out.file.SpreadsheetTableExtractor;
import com.is.bcs.adapter.out.geo.Proj4jCoordinateTransformer;
import com.is.bcs.application.dto.ControlPointPreviewResult;
import com.is.bcs.application.dto.ControlPointPreviewResult.Action;
import com.is.bcs.application.dto.ImportPreviewResult;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import com.is.bcs.support.FakeControlPointStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 파일 미리보기 검증 — 대상지는 건수·열 매핑·오류를, 기준점은 저장소 대조(신규/갱신·충돌·행 경고)까지 돌려준다. */
class SurveyCsvPreviewServiceTest {

    private final FakeControlPointStore pointStore = new FakeControlPointStore();
    // 좌표계 정의는 초기화 이후 읽기만 하므로 하나를 나눠 쓴다
    private final Proj4jCoordinateTransformer transformer = new Proj4jCoordinateTransformer();
    private final SurveyCsvPreviewService service = new SurveyCsvPreviewService(
            new SpreadsheetTableExtractor(),
            new SurveyTargetMapper(transformer),
            new ControlPointFileMapper(transformer),
            new ControlPointRegistrar(pointStore, pointStore));

    private byte[] sampleCsv() throws Exception {
        try (var in = getClass().getResourceAsStream("/survey-target-sample.csv")) {
            return in.readAllBytes();
        }
    }

    @Test
    @DisplayName("실파일 — 49행, 인식한 열과 값만 보관하는 열을 알리고 오류는 없다")
    void preview_realFile() throws Exception {
        ImportPreviewResult result = service.preview(sampleCsv());

        assertEquals(49, result.totalRows());
        assertTrue(result.errors().isEmpty());
        assertEquals("기존조사내용", result.recognizedColumns().get("기존조사내"));
        assertEquals("조사대상여부", result.recognizedColumns().get("조사대상여"));
        assertEquals(List.of("순번", "field_20"), result.extraColumns());
    }

    @Test
    @DisplayName("잘못된 행이 있어도 끝까지 읽어 오류를 모두 돌려준다")
    void preview_collectsAllRowErrors() {
        String csv = """
                기준점번호,종류,기준점명,좌표계구분,X좌표,Y좌표,조사대상여부,토지소재지,상세주소,설치일자,기존조사내용,기존조사일
                41192D000000001,도근점,정상,세계,545000,181000,대상
                41192D000000002,수준점,이상1,세계,545000,181000,대상
                41192D000000003,도근점,이상2,세계,좌표아님,181000,대상
                """;

        ImportPreviewResult result = service.preview(csv.getBytes(StandardCharsets.UTF_8));

        assertEquals(3, result.totalRows());
        assertEquals(2, result.errors().size());
        assertEquals(3, result.errors().getFirst().row());
        assertEquals(4, result.errors().getLast().row());
    }

    @Test
    @DisplayName("필수 열이 없으면 미리보기 자체가 거부된다 — 파일 전체를 읽을 수 없다")
    void preview_missingRequiredColumns_throws() {
        byte[] csv = "종류,기준점명\n도근점,이름\n".getBytes(StandardCharsets.UTF_8);

        InvalidControlPointException thrown =
                assertThrows(InvalidControlPointException.class, () -> service.preview(csv));

        assertTrue(thrown.getMessage().contains("기준점번호"), thrown.getMessage());
    }

    @Test
    @DisplayName("기준점 미리보기 — 점마다 새로 등록되는지 기존 값이 덮이는지 알린다")
    void previewControlPoints_tellsWhatHappensToEachPoint() throws Exception {
        // 파일과 이름·종류는 같고 성과만 다른 점을 미리 넣어 둔다 — 이 점은 갱신 대상이 된다
        pointStore.save(ControlPoint.register(
                "41192D000001265", PointType.DOGEUN, "1465공",
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL, new BigDecimal("545000.00"), new BigDecimal("181000.00")),
                new GeoCoordinate(126.790000, 37.500000),
                null, null, null, null, null, null, null, null, null));

        ControlPointPreviewResult result = service.previewControlPoints(sampleCsv());

        assertEquals(49, result.points().size());
        var updated = result.points().stream().filter(p -> p.action() == Action.UPDATE).toList();
        assertEquals(1, updated.size());
        assertEquals("1465공", updated.getFirst().name());
        // 무엇이 어떻게 바뀌는지까지 — 화면이 그대로 보여 준다
        assertTrue(updated.getFirst().changes().stream().anyMatch(c -> c.field().equals("X좌표")
                && c.before().equals("545000.0000") && c.after().equals("545236.7700")), updated.getFirst().changes().toString());
        assertEquals(48, result.points().stream().filter(p -> p.action() == Action.NEW).count());
    }

    @Test
    @DisplayName("파일의 관리번호를 다른 이름의 점이 쓰고 있으면 미리보기가 행 오류로 알린다 — 등록을 눌러야 아는 실패를 없앤다")
    void previewControlPoints_pointNoConflict_isRowError() {
        pointStore.save(ControlPoint.register(
                "41192D000000001", PointType.DOGEUN, "선점",
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL, new BigDecimal("545000.00"), new BigDecimal("181000.00")),
                new GeoCoordinate(126.790000, 37.500000),
                null, null, null, null, null, null, null, null, null));

        String csv = """
                기준점번호,종류,기준점명,좌표계구분,X좌표,Y좌표,토지소재지,상세주소,설치일자,기존조사내용,기존조사일
                41192D000000001,도근점,새점,세계,545100,181100
                """;
        ControlPointPreviewResult result = service.previewControlPoints(csv.getBytes(StandardCharsets.UTF_8));

        assertEquals(1, result.file().errors().size());
        assertTrue(result.file().errors().getFirst().message().contains("선점"),
                result.file().errors().getFirst().message());
        assertTrue(result.points().isEmpty()); // 등록되지 않을 행은 점 목록에 세우지 않는다
    }

    @Test
    @DisplayName("부천 범위 밖 행 경고는 그 점의 줄에 붙는다")
    void previewControlPoints_attachesWarningToPoint() {
        String csv = """
                기준점번호,종류,기준점명,좌표계구분,X좌표,Y좌표,토지소재지,상세주소,설치일자,기존조사내용,기존조사일
                41192D000000002,도근점,멀리,세계,445000,181000
                """;
        ControlPointPreviewResult result = service.previewControlPoints(csv.getBytes(StandardCharsets.UTF_8));

        assertEquals(1, result.points().size());
        assertNotNull(result.points().getFirst().warning());
        assertTrue(result.points().getFirst().warning().contains("범위 밖"), result.points().getFirst().warning());
        assertEquals(1, result.file().warnings().size()); // 파일 단위 목록에도 그대로 남는다
    }

    @Test
    @DisplayName("파일의 최종조사 열은 갱신 항목으로 잡히고, 그 열이 없는 파일은 기존 요약을 지우지 않는다")
    void previewControlPoints_lastSurveyDiffOnlyWhenFileHasValue() {
        // 성과·속성이 파일 행과 완전히 같은 점 — 최종조사 요약만 이미 들고 있다
        var tm = new TmCoordinate(
                CoordinateSystem.GRS80_CENTRAL, new BigDecimal("545100.00"), new BigDecimal("181100.00"));
        pointStore.save(ControlPoint.register(
                "41192D000000003", PointType.DOGEUN, "그대로", tm,
                transformer.toWgs84(tm),
                null, null, null, null, null, null, null,
                "망실", LocalDate.of(2025, 9, 8)));

        String withoutColumn = """
                기준점번호,종류,기준점명,좌표계구분,X좌표,Y좌표,토지소재지,상세주소,설치일자,기존조사내용,기존조사일
                41192D000000003,도근점,그대로,세계,545100.00,181100.00
                """;
        ControlPointPreviewResult kept = service.previewControlPoints(withoutColumn.getBytes(StandardCharsets.UTF_8));
        assertEquals(Action.UNCHANGED, kept.points().getFirst().action()); // 열이 없다 = 모른다, 갱신 아님

        String withColumn = """
                기준점번호,종류,기준점명,좌표계구분,X좌표,Y좌표,최종조사내용,최종조사일자,토지소재지,상세주소,설치일자,기존조사내용,기존조사일
                41192D000000003,도근점,그대로,세계,545100.00,181100.00,완전,2026-06-23
                """;
        ControlPointPreviewResult changed = service.previewControlPoints(withColumn.getBytes(StandardCharsets.UTF_8));
        assertEquals(Action.UPDATE, changed.points().getFirst().action());
        assertTrue(changed.points().getFirst().changes().stream().anyMatch(c ->
                        // 파일이 "완전"이라 적어도 화면 어휘인 "정상"으로 옮겨 담는다
                        c.field().equals("최종조사내용") && c.before().equals("망실") && c.after().equals("정상")),
                changed.points().getFirst().changes().toString());
    }
}
