package com.is.bcs.adapter.out.geo;

import com.is.bcs.application.port.out.geo.CoordinateTransformer;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * proj4j 로 TM 성과를 경위도로 변환한다.
 * 좌표계는 EPSG 코드가 아니라 투영 파라미터로 직접 정의한다 — EPSG 데이터베이스 의존을 더하지 않으려는 것이고,
 * 원점·타원체가 코드에 드러나 있어야 어느 원점을 쓰는지 눈으로 확인할 수 있다.
 *
 * 공유 상태는 불변인 좌표계 정의뿐이라 여러 요청이 동시에 불러도 안전하다.
 */
@Component
public class Proj4jCoordinateTransformer implements CoordinateTransformer {

    private static final String WGS84 = "+proj=longlat +datum=WGS84 +no_defs";

    /**
     * 공통: lat_0=38, k=1, x_0=200000. 원점마다 lon_0 이 다르다.
     * 동경측지계(EPSG:5174)는 타원체(bessel)와 y_0(500000)이 다르고, 세계측지계와의 차이를 towgs84 7파라미터로 보정한다.
     */
    private static final Map<CoordinateSystem, String> DEFINITIONS = new EnumMap<>(Map.of(
            CoordinateSystem.GRS80_WEST,
            "+proj=tmerc +lat_0=38 +lon_0=125 +k=1 +x_0=200000 +y_0=600000 +ellps=GRS80 +units=m +no_defs",
            CoordinateSystem.GRS80_CENTRAL,
            "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=600000 +ellps=GRS80 +units=m +no_defs",
            CoordinateSystem.GRS80_EAST,
            "+proj=tmerc +lat_0=38 +lon_0=129 +k=1 +x_0=200000 +y_0=600000 +ellps=GRS80 +units=m +no_defs",
            CoordinateSystem.GRS80_EAST_SEA,
            "+proj=tmerc +lat_0=38 +lon_0=131 +k=1 +x_0=200000 +y_0=600000 +ellps=GRS80 +units=m +no_defs",
            CoordinateSystem.BESSEL_CENTRAL,
            "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m"
                    + " +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43"));

    private final Map<CoordinateSystem, CoordinateReferenceSystem> sources = new EnumMap<>(CoordinateSystem.class);
    private final CoordinateReferenceSystem target;

    public Proj4jCoordinateTransformer() {
        CRSFactory factory = new CRSFactory();
        target = factory.createFromParameters("WGS84", WGS84);
        DEFINITIONS.forEach((crs, definition) ->
                sources.put(crs, factory.createFromParameters(crs.getEpsgCode(), definition)));

        // 좌표계를 새로 등록하면서 정의를 빠뜨리면 그 성과를 실제로 만나기 전까지 드러나지 않는다 — 기동할 때 막는다
        List<CoordinateSystem> undefined = Arrays.stream(CoordinateSystem.values())
                .filter(crs -> !sources.containsKey(crs))
                .toList();
        if (!undefined.isEmpty()) {
            throw new IllegalStateException("변환 정의가 없는 좌표계가 있습니다: " + undefined);
        }
    }

    @Override
    public GeoCoordinate toWgs84(TmCoordinate tm) {
        CoordinateReferenceSystem source = sources.get(tm.crs());
        if (source == null) {
            // 좌표계를 새로 등록하면서 정의를 빠뜨린 경우 — 조용히 어긋난 좌표를 만들지 않고 멈춘다
            throw new InvalidControlPointException("변환 정의가 없는 좌표계입니다: " + tm.crs().getEpsgCode());
        }

        // 측량 성과는 X=북·Y=동이지만 proj4j 는 GIS 순서(x=동, y=북)로 받는다
        ProjCoordinate from = new ProjCoordinate(tm.easting().doubleValue(), tm.northing().doubleValue());
        ProjCoordinate converted = new ProjCoordinate();

        // 변환기는 내부 스크래치 좌표를 재사용해 스레드 안전하지 않다 → 필드에 두지 않고 호출마다 만든다.
        // 좌표계 정의는 초기화 이후 읽기만 하므로 공유해도 된다.
        new BasicCoordinateTransform(source, target).transform(from, converted);

        return new GeoCoordinate(converted.x, converted.y);
    }
}
