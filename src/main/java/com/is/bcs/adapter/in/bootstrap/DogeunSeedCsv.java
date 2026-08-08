package com.is.bcs.adapter.in.bootstrap;

import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TmCoordinate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 부천 도근점 시드 파일 로더 — 원본은 지적도근점 5174 성과(shp)에서 추출한 2,146점
 * (동경측지계 성과 보존, 경위도는 datum 변환 파생, 교차점·좌표 이상 제외).
 */
public final class DogeunSeedCsv {

    private static final String RESOURCE = "/seed/dogeun-bucheon.csv";

    private DogeunSeedCsv() {
    }

    public static List<ControlPoint> load() {
        String text;
        try (InputStream in = DogeunSeedCsv.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("시드 파일이 없습니다: " + RESOURCE);
            }
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return text.lines()
                .skip(1) // 헤더: pointNo,name,lng,lat,easting,northing
                .filter(line -> !line.isBlank())
                .map(DogeunSeedCsv::toPoint)
                .toList();
    }

    private static ControlPoint toPoint(String line) {
        String[] cells = line.split(",");
        return ControlPoint.register(
                cells[0], PointType.DOGEUN, cells[1],
                new TmCoordinate(CoordinateSystem.BESSEL_CENTRAL,
                        new BigDecimal(cells[5]), new BigDecimal(cells[4])), // northing, easting
                new GeoCoordinate(Double.parseDouble(cells[2]), Double.parseDouble(cells[3])),
                null, null, null, null, null, null, null,
                null, null
        );
    }
}
