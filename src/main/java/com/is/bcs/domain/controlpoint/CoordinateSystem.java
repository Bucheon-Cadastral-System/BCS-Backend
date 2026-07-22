package com.is.bcs.domain.controlpoint;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 지적 성과 좌표의 TM 원점 좌표계. 성과 좌표는 반드시 좌표계와 함께 저장한다 —
 * 값만으로는 어느 원점 기준인지 알 수 없어 좌표계 없는 성과는 재현 불가능한 숫자가 된다.
 * 부천 = GRS80_CENTRAL(중부원점). BESSEL_CENTRAL은 2007년 이전 구(동경측지계) 성과 혼입 대비.
 */
@Getter
@RequiredArgsConstructor
public enum CoordinateSystem {

    GRS80_WEST("EPSG:5185", "서부원점(세계측지계)"),
    GRS80_CENTRAL("EPSG:5186", "중부원점(세계측지계)"),
    GRS80_EAST("EPSG:5187", "동부원점(세계측지계)"),
    GRS80_EAST_SEA("EPSG:5188", "동해원점(세계측지계)"),
    BESSEL_CENTRAL("EPSG:5174", "중부원점(구·동경측지계)");

    private final String epsgCode;
    private final String displayName;
}
