package com.is.bcs.domain.controlpoint;

import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;

/**
 * 지도 표시용 경위도(WGS84) — TM 성과에서 변환한 파생값이며 권위값이 아니다.
 * 변환 오류로 깨진 좌표(범위 밖)가 저장되는 것만 막는다.
 */
public record GeoCoordinate(double longitude, double latitude) {

    public GeoCoordinate {
        // NaN은 모든 범위 비교를 통과하므로 유한수 검증을 먼저 한다
        if (!Double.isFinite(longitude) || !Double.isFinite(latitude)
                || longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) {
            throw new InvalidControlPointException(
                    "경위도 범위를 벗어났습니다: " + longitude + ", " + latitude);
        }
    }
}
