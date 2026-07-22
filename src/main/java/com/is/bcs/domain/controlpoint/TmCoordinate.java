package com.is.bcs.domain.controlpoint;

import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;

import java.math.BigDecimal;

/**
 * TM 원점 기준 공식 성과 좌표(권위값, 미터). 경위도는 이 값에서 변환한 표시용 파생이다.
 *
 * 축 관례 주의: 측량 성과의 X=북방향(northing)·Y=동방향(easting)으로, GIS의 (x=동, y=북)과 반대다.
 * 혼동을 없애기 위해 필드명을 X/Y가 아니라 northing/easting으로 강제한다.
 * 값은 cm 단위 정밀도를 오차 없이 보존해야 하므로 BigDecimal을 쓴다.
 */
public record TmCoordinate(CoordinateSystem crs, BigDecimal northing, BigDecimal easting) {

    public TmCoordinate {
        if (crs == null || northing == null || easting == null) {
            throw new InvalidControlPointException("성과 좌표는 좌표계·북방향(X)·동방향(Y) 값이 모두 필요합니다.");
        }
    }
}
