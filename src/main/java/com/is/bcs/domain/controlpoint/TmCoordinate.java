package com.is.bcs.domain.controlpoint;

import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * TM 원점 기준 공식 성과 좌표(권위값, 미터). 경위도는 이 값에서 변환한 표시용 파생이다.
 *
 * 축 관례 주의: 측량 성과의 X=북방향(northing)·Y=동방향(easting)으로, GIS의 (x=동, y=북)과 반대다.
 * 혼동을 없애기 위해 필드명을 X/Y가 아니라 northing/easting으로 강제한다.
 * 값은 정밀도를 오차 없이 보존해야 하므로 BigDecimal을 쓴다.
 *
 * 자릿수는 여기서 확정한다. 저장소가 자기 자릿수로 잘라 버리면 같은 파일을 다시 올려도 값이 달라 보여
 * 매번 갱신 대상으로 잡히고, 등록해도 같은 자리에서 다시 잘려 영원히 수렴하지 않는다.
 */
public record TmCoordinate(CoordinateSystem crs, BigDecimal northing, BigDecimal easting) {

    /** 성과 좌표의 소수 자릿수 — 고객사 성과가 0.1mm까지 적혀 온다. 저장소 컬럼도 이 값을 따른다. */
    public static final int SCALE = 4;

    public TmCoordinate {
        if (crs == null || northing == null || easting == null) {
            throw new InvalidControlPointException("성과 좌표는 좌표계·북방향(X)·동방향(Y) 값이 모두 필요합니다.");
        }
        northing = northing.setScale(SCALE, RoundingMode.HALF_UP);
        easting = easting.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
