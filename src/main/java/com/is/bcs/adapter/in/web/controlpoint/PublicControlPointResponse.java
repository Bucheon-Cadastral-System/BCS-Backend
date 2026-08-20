package com.is.bcs.adapter.in.web.controlpoint;

import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.PointType;

import java.math.BigDecimal;

/**
 * 로그인 없이 보는 기준점 한 점.
 *
 * <p>회원 응답({@link ControlPointResponse})이 싣는 값을 넘지 않는다. 성과(TM 좌표)까지가 화면이 그리는
 * 값이고, 소재지·법정동·표지재질 같은 값은 파일이 들고 왔을 뿐 로그인해도 볼 자리가 없어 여기에도 싣지 않는다.
 * 반대로 설치일자·판 번호는 관리하는 쪽의 값이라 회원 응답에만 남긴다.
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
        double latitude
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
                point.getGeo().latitude()
        );
    }
}
