package com.is.bcs.application.dto;

import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TraverseInfo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 경위도는 받지 않는다 — 권위값(TM 성과)에서 서버가 파생해야 두 값이 어긋날 길이 없다. */
public record RegisterControlPointCommand(
        String pointNo,
        PointType type,
        String name,
        CoordinateSystem crs,
        BigDecimal northing,
        BigDecimal easting,
        String regionCode,
        String regionName,
        String address,
        MarkerMaterial markerMaterial,
        InstallType installType,
        LocalDate installedDate,
        TraverseInfo traverse
) {
}
