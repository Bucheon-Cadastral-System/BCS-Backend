package com.is.bcs.adapter.in.ai;

import com.is.bcs.domain.controlpoint.ControlPoint;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 기준점 단건 상세(모델용) — enum은 한글 표시명으로 푼다. */
public record ControlPointDetail(
        String pointNo, String name, String type, String crs,
        BigDecimal northing, BigDecimal easting,
        double longitude, double latitude,
        String regionName, String address,
        String markerMaterial, String installType, LocalDate installedDate
) {

    public static ControlPointDetail from(ControlPoint point) {
        return new ControlPointDetail(
                point.getPointNo(), point.getName(), point.getType().getDisplayName(),
                point.getTm().crs().getDisplayName(),
                point.getTm().northing(), point.getTm().easting(),
                point.getGeo().longitude(), point.getGeo().latitude(),
                point.getRegionName(), point.getAddress(),
                point.getMarkerMaterial() == null ? null : point.getMarkerMaterial().getDisplayName(),
                point.getInstallType() == null ? null : point.getInstallType().getDisplayName(),
                point.getInstalledDate());
    }
}
