package com.is.bcs.application.service;

import com.is.bcs.application.service.ExcavationCsvParser.Row;
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
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 굴착협의 CSV 파싱 검증 — 픽스처는 고객사 실파일(EUC-KR) 그대로. */
class ExcavationCsvParserTest {

    private static final Charset EUC_KR = Charset.forName("EUC-KR");

    private byte[] sampleCsv() throws Exception {
        try (var in = getClass().getResourceAsStream("/excavation-sample.csv")) {
            return in.readAllBytes();
        }
    }

    @Test
    @DisplayName("실파일 49행이 전부 파싱되고 첫 행의 모든 필드가 원본 값과 일치한다")
    void parse_realFile_firstRow() throws Exception {
        List<Row> rows = ExcavationCsvParser.parse(sampleCsv());

        assertEquals(49, rows.size());

        Row first = rows.get(0);
        assertEquals("41192D000001265", first.pointNo());
        assertEquals(PointType.DOGEUN, first.type());
        assertEquals("1465공", first.name());
        assertEquals(CoordinateSystem.GRS80_CENTRAL, first.crs());
        assertEquals(new BigDecimal("545236.77"), first.northing()); // X좌표=북방향
        assertEquals(new BigDecimal("181840.96"), first.easting()); // Y좌표=동방향
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
    void parse_realFile_vocabularyCounts() throws Exception {
        List<Row> rows = ExcavationCsvParser.parse(sampleCsv());

        assertEquals(40, rows.stream().filter(r -> r.priorResult() == SurveyResult.INTACT).count());
        assertEquals(3, rows.stream().filter(r -> r.priorResult() == SurveyResult.LOST).count());
        assertEquals(1, rows.stream().filter(r -> r.priorResult() == SurveyResult.ETC).count());
        assertEquals(5, rows.stream().filter(r -> r.priorResult() == null).count());
        assertEquals(1, rows.stream().filter(r -> r.type() == PointType.TRIANGULATION_AUX).count());
        assertEquals(48, rows.stream().filter(r -> r.type() == PointType.DOGEUN).count());
    }

    @Test
    @DisplayName("교차구분 라벨 — '도근점'은 일반(false), 빈값은 미기재(null)로 해석한다")
    void parse_intersectionLabel() throws Exception {
        List<Row> rows = ExcavationCsvParser.parse(sampleCsv());

        assertEquals(42, rows.stream()
                .filter(r -> r.traverse() != null && Boolean.FALSE.equals(r.traverse().intersection()))
                .count());
        assertNull(rows.get(0).traverse().intersection()); // 행1은 교차구분 빈값
    }

    @Test
    @DisplayName("알 수 없는 어휘(종류·좌표계 등)는 행 번호와 함께 거부한다")
    void parse_unknownVocabulary_throws() {
        String header = "순번,종류,기준점명,조사대상여,기준점번호,좌표계구분,X좌표,Y좌표,토지소재지,상세주소,표지재질,도선등급,도선명,도호,교차구분,설치구분,설치일자,기존조사내,기존조사일,field_20,경도(X),위도(Y)";
        String badType = "1,수준점,이름,,41192D000000001,세계,545000,181000,10100-원미동,주소,철재,,,,,설치,2018-01-01,,,,126.79,37.50";

        byte[] csv = (header + "\n" + badType).getBytes(EUC_KR);

        assertThrows(InvalidControlPointException.class, () -> ExcavationCsvParser.parse(csv));
    }
}
