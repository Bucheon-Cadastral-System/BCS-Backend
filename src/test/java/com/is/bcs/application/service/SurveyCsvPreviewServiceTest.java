package com.is.bcs.application.service;

import com.is.bcs.adapter.out.file.SpreadsheetTableExtractor;
import com.is.bcs.application.dto.SurveyCsvPreviewResult;
import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 대상지 파일 미리보기 검증 — 등록하지 않고 건수·열 매핑·오류만 돌려준다. */
class SurveyCsvPreviewServiceTest {

    private final SurveyCsvPreviewService service = new SurveyCsvPreviewService(new SpreadsheetTableExtractor());

    private byte[] sampleCsv() throws Exception {
        try (var in = getClass().getResourceAsStream("/survey-target-sample.csv")) {
            return in.readAllBytes();
        }
    }

    @Test
    @DisplayName("실파일 — 49행, 인식한 열과 무시한 열을 알리고 오류는 없다")
    void preview_realFile() throws Exception {
        SurveyCsvPreviewResult result = service.preview(sampleCsv(), Map.of());

        assertEquals(49, result.totalRows());
        assertTrue(result.errors().isEmpty());
        assertEquals("기존조사내용", result.recognizedColumns().get("기존조사내"));
        assertEquals("조사대상여부", result.recognizedColumns().get("조사대상여"));
        assertTrue(result.ignoredColumns().contains("순번"));
        assertTrue(result.ignoredColumns().contains("field_20"));
        // 담당자가 무시된 열을 이어 붙일 수 있도록 고를 수 있는 항목을 함께 준다
        assertTrue(result.assignableColumns().contains("설치일자"), result.assignableColumns().toString());
    }

    @Test
    @DisplayName("잘못된 행이 있어도 끝까지 읽어 오류를 모두 돌려준다")
    void preview_collectsAllRowErrors() {
        String csv = """
                기준점번호,종류,기준점명,좌표계구분,X좌표,Y좌표
                41192D000000001,도근점,정상,세계,545000,181000
                41192D000000002,수준점,이상1,세계,545000,181000
                41192D000000003,도근점,이상2,세계,좌표아님,181000
                """;

        SurveyCsvPreviewResult result = service.preview(csv.getBytes(StandardCharsets.UTF_8), Map.of());

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
                assertThrows(InvalidControlPointException.class, () -> service.preview(csv, Map.of()));

        assertTrue(thrown.getMessage().contains("기준점번호"), thrown.getMessage());
    }
}
