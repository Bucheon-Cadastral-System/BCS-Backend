package com.is.bcs.adapter.out.geo;

import com.is.bcs.adapter.out.file.SpreadsheetTableExtractor;
import com.is.bcs.application.service.SurveyTargetMapper;
import com.is.bcs.application.service.SurveyTargetMapper.Row;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 성과 좌표 → 경위도 변환 검증.
 * 정답지는 고객사 실파일이다 — 같은 행에 TM 성과와 경위도가 함께 들어 있어 변환 결과를 대조할 수 있다.
 */
class Proj4jCoordinateTransformerTest {

    /** 1e-6도 ≈ 9cm. 실측 편차는 4e-7도(약 4cm) 수준이라 이 폭이면 회귀를 잡으면서 오탐은 없다. */
    private static final double TOLERANCE_DEGREES = 1e-6;

    private final Proj4jCoordinateTransformer transformer = new Proj4jCoordinateTransformer();

    private byte[] sampleCsv() throws Exception {
        try (var in = getClass().getResourceAsStream("/survey-target-sample.csv")) {
            return in.readAllBytes();
        }
    }

    @Test
    @DisplayName("실파일 49행의 성과 좌표를 변환하면 같은 행에 적힌 경위도와 일치한다")
    void toWgs84_matchesLongLatInFile() throws Exception {
        List<Row> rows = SurveyTargetMapper.map(new SpreadsheetTableExtractor().extract(sampleCsv())).rows();

        // 행이 사라지면 아래 반복이 통째로 건너뛰어져 회귀를 놓친다
        assertEquals(49, rows.size());
        for (Row row : rows) {
            GeoCoordinate geo = transformer.toWgs84(new TmCoordinate(row.crs(), row.northing(), row.easting()));

            assertEquals(row.longitude(), geo.longitude(), TOLERANCE_DEGREES, row.name() + " 경도");
            assertEquals(row.latitude(), geo.latitude(), TOLERANCE_DEGREES, row.name() + " 위도");
        }
    }
}
