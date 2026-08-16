package com.is.bcs.adapter.in.web.controlpoint;

import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.PointType;

public record PublicControlPointResponse(
        Long id,
        String pointNo,
        PointType type,
        String name,
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
                point.getGeo().longitude(),
                point.getGeo().latitude(),
                point.getRegionCode(),
                point.getRegionName(),
                point.getAddress()
        );
    }
}