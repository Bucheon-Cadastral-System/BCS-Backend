package com.is.bcs.adapter.out.file;

import com.is.bcs.application.port.out.file.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 파일 → 표 추출 검증.
 * 같은 내용을 CSV·XLSX로 만들어 두고, 어느 쪽으로 올려도 뒤 단계가 같은 표를 받는지 본다.
 */
class SpreadsheetTableExtractorTest {

    private final SpreadsheetTableExtractor extractor = new SpreadsheetTableExtractor();

    private byte[] resource(String name) throws Exception {
        try (var in = getClass().getResourceAsStream(name)) {
            return in.readAllBytes();
        }
    }

    @Test
    @DisplayName("EUC-KR CSV 에서 헤더 22열과 데이터 49행을 뽑는다")
    void extract_eucKrCsv() throws Exception {
        Table table = extractor.extract(resource("/survey-target-sample.csv"));

        assertEquals(22, table.headers().size());
        assertEquals("순번", table.headers().getFirst());
        assertEquals("위도(Y)", table.headers().getLast());
        assertEquals(49, table.rows().size());
        assertEquals("41192D000001265", table.rows().getFirst().get(table.headers().indexOf("기준점번호")));
    }

    @Test
    @DisplayName("UTF-8 BOM CSV 도 같은 표로 읽히고 헤더에 BOM 이 섞이지 않는다")
    void extract_utf8BomCsv() throws Exception {
        Table table = extractor.extract(resource("/survey-target-utf8bom.csv"));

        assertEquals("순번", table.headers().getFirst());
        assertEquals(49, table.rows().size());
    }

    @Test
    @DisplayName("XLSX 도 CSV 와 같은 표로 읽힌다 — 날짜는 시리얼이 아닌 날짜 문자열, 좌표는 자릿수 그대로")
    void extract_xlsx() throws Exception {
        Table csv = extractor.extract(resource("/survey-target-sample.csv"));
        Table xlsx = extractor.extract(resource("/survey-target-sample.xlsx"));

        assertEquals(csv.headers(), xlsx.headers());
        assertEquals(csv.rows().size(), xlsx.rows().size());

        List<String> first = xlsx.rows().getFirst();
        assertEquals("545236.77", first.get(xlsx.headers().indexOf("X좌표")));
        assertEquals("181840.96", first.get(xlsx.headers().indexOf("Y좌표")));
        assertEquals("2018-02-21", first.get(xlsx.headers().indexOf("설치일자")));
        assertEquals("2025-09-08", first.get(xlsx.headers().indexOf("기존조사일")));
    }

    @Test
    @DisplayName("헤더가 첫 행이 아닌 XLSX 는 제목·빈 행을 건너뛰고 헤더 행을 찾는다")
    void extract_xlsxWithTitleRows() throws Exception {
        Table plain = extractor.extract(resource("/survey-target-sample.xlsx"));
        Table titled = extractor.extract(resource("/survey-target-titled.xlsx"));

        assertEquals(plain.headers(), titled.headers());
        assertEquals(plain.rows().size(), titled.rows().size());
    }
}
