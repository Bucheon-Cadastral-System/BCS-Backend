package com.is.bcs.application.dto;

import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TraverseInfo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterControlPointCommand(
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
        TraverseInfo traverse
) {
}
