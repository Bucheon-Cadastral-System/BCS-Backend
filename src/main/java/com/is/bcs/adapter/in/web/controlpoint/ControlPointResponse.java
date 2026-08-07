package com.is.bcs.adapter.in.web.controlpoint;

import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TraverseInfo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ControlPointResponse(
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
        String address,
        MarkerMaterial markerMaterial,
        InstallType installType,
        LocalDate installedDate,
        TraverseResponse traverse,
        // 최종조사 요약(결과·조사일·조사원)은 여기 싣지 않는다.
        // 점 하나를 고른 뒤에만 보이는 값이라 목록에 실으면 화면이 쓰지 않는 값을 수천 행만큼 나른다.
        /** 판 번호 — 수정 요청이 그대로 돌려보내 그사이 다른 사람이 먼저 고쳤는지 가린다 */
        long version
) {

    public record TraverseResponse(String grade, String lineName, String lineNo, Boolean intersection) {

        static TraverseResponse from(TraverseInfo traverse) {
            if (traverse == null) {
                return null;
            }
            return new TraverseResponse(
                    traverse.grade(), traverse.lineName(), traverse.lineNo(), traverse.intersection());
        }
    }

    public static ControlPointResponse from(ControlPoint point) {
        return new ControlPointResponse(
                point.getId(), point.getPointNo(), point.getType(), point.getName(),
                point.getTm().crs(), point.getTm().northing(), point.getTm().easting(),
                point.getGeo().longitude(), point.getGeo().latitude(),
                point.getRegionCode(), point.getRegionName(), point.getAddress(),
                point.getMarkerMaterial(), point.getInstallType(), point.getInstalledDate(),
                TraverseResponse.from(point.getTraverse()), point.getVersion()
        );
    }
}
