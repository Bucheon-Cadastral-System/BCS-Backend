package com.is.bcs.adapter.in.web.controlpoint;

import com.is.bcs.application.dto.RegisterControlPointCommand;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TraverseInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 경위도는 받지 않는다 — 권위값(TM 성과)에서 서버가 파생한다. */
public record RegisterControlPointRequest(
        @NotBlank(message = "관리번호는 필수입니다.") String pointNo,
        @NotNull(message = "종류는 필수입니다.") PointType type,
        @NotBlank(message = "기준점명은 필수입니다.") String name,
        @NotNull(message = "좌표계는 필수입니다.") CoordinateSystem crs,
        @NotNull(message = "북방향(X) 성과는 필수입니다.") BigDecimal northing,
        @NotNull(message = "동방향(Y) 성과는 필수입니다.") BigDecimal easting,
        String regionCode,
        String regionName,
        String address,
        MarkerMaterial markerMaterial,
        InstallType installType,
        LocalDate installedDate,
        TraverseRequest traverse
) {

    public record TraverseRequest(String grade, String lineName, String lineNo, Boolean intersection) {

        TraverseInfo toDomain() {
            return new TraverseInfo(grade, lineName, lineNo, intersection);
        }
    }

    public RegisterControlPointCommand toCommand() {
        return new RegisterControlPointCommand(
                pointNo, type, name, crs, northing, easting,
                regionCode, regionName, address, markerMaterial, installType, installedDate,
                traverse != null ? traverse.toDomain() : null
        );
    }
}
