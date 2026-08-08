package com.is.bcs.domain.controlpoint;

/**
 * 이 시스템이 관리하는 지역의 범위.
 * 성과의 좌표계를 잘못 적은 파일은 값 자체는 숫자로 읽히므로 형식 검사를 통과하지만, 변환하면 엉뚱한 곳을 가리킨다 —
 * 그런 행을 막지는 않고 확인 요청으로 알린다(관리 지역이 넓어질 수 있고, 값의 정오는 성과를 가진 쪽이 판단한다).
 *
 * 경계선이 아니라 여유를 둔 사각형으로 잡는다. 행정구역 경계와의 대조는 공간 질의가 필요한데,
 * 여기서 걸러야 하는 것은 경계에서 몇 미터 벗어난 점이 아니라 원점이 달라 수십 킬로미터 어긋난 점이다.
 */
public record ServiceArea(
        String name, double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {

    public static final ServiceArea BUCHEON = new ServiceArea("부천시", 37.44, 37.58, 126.68, 126.88);

    public boolean contains(GeoCoordinate coordinate) {
        return coordinate.latitude() >= minLatitude && coordinate.latitude() <= maxLatitude
                && coordinate.longitude() >= minLongitude && coordinate.longitude() <= maxLongitude;
    }
}
