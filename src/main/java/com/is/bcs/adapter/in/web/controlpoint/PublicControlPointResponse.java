package com.is.bcs.adapter.in.web.controlpoint;

import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.PointType;

import java.math.BigDecimal;

/**
 * 로그인 없이 보는 기준점 한 점.
 *
 * <p>성과(TM 좌표)는 싣고 관리 정보는 싣지 않는다. 지적기준점 성과는 열람이 열려 있는 값이고,
 * 같은 자리를 이미 경위도로 내리고 있어 감추어도 가려지는 것이 없다. 반면 설치일자·판 번호·조사 이력은
 * 관리하는 쪽의 값이라 공개 응답에 두지 않는다.
 */
public record PublicControlPointResponse(
        Long id,
        String pointNo,
        PointType type,
        String name,
        CoordinateSystem crs,
        BigDecimal northing,
        BigDecimal easting,
        double longitude,
        double latitude,
        String regionCode,
        String regionName,
        String address
) {

    public static PublicControlPointResponse from(ControlPoint point) {
        return new PublicControlPointResponse(
                point.getId(),
                point.getPointNo(),
                point.getType(),
                point.getName(),
                point.getTm().crs(),
                point.getTm().northing(),
                point.getTm().easting(),
                point.getGeo().longitude(),
                point.getGeo().latitude(),
                point.getRegionCode(),
                point.getRegionName(),
                point.getAddress()
        );
    }
}
