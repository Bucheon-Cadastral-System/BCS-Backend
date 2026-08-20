package com.is.bcs.adapter.out.file;

import com.is.bcs.application.port.out.file.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 내보낸 파일을 그대로 다시 읽을 수 있어야 한다 — 다음 회차를 이 파일로 여는 것이 이 기능의 쓰임이다. */
class XlsxTableWriterTest {

    private final XlsxTableWriter writer = new XlsxTableWriter();
    private final SpreadsheetTableExtractor extractor = new SpreadsheetTableExtractor();

    @Test
    @DisplayName("쓴 표를 다시 읽으면 같은 표가 나온다")
    void roundTrip() {
        Table table = new Table(
                List.of("기준점번호", "X좌표", "설치일자"),
                List.of(
                        List.of("41192D000001265", "545236.77", "2018-02-21"),
                        List.of("41192D000001267", "545201.7401", "")));

        Table read = extractor.extract(writer.write("대상 기준점", table));

        assertEquals(table.headers(), read.headers());
        assertEquals("41192D000001265", read.rows().getFirst().getFirst());
        assertEquals("545236.77", read.rows().getFirst().get(1));
        assertEquals("2018-02-21", read.rows().getFirst().get(2));
        assertEquals("545201.7401", read.rows().get(1).get(1));
        assertEquals("", read.rows().get(1).get(2));
    }
}
