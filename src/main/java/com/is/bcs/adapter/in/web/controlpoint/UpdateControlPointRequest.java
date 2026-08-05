package com.is.bcs.adapter.in.web.controlpoint;

import com.is.bcs.application.dto.UpdateControlPointCommand;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.PointType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 기준점 수정 — 식별·성과만 받는다. 요청에 없는 소재지·설치·최종조사 항목은 서버가 기존 값을 유지한다.
 * 경위도는 받지 않는다 — 권위값(TM 성과)에서 서버가 파생한다.
 */
public record UpdateControlPointRequest(
        @NotBlank(message = "관리번호는 필수입니다.") String pointNo,
        @NotNull(message = "종류는 필수입니다.") PointType type,
        @NotBlank(message = "기준점명은 필수입니다.") String name,
        @NotNull(message = "좌표계는 필수입니다.") CoordinateSystem crs,
        @NotNull(message = "북방향(X) 성과는 필수입니다.") BigDecimal northing,
        @NotNull(message = "동방향(Y) 성과는 필수입니다.") BigDecimal easting
) {

    public UpdateControlPointCommand toCommand(Long pointId) {
        return new UpdateControlPointCommand(pointId, pointNo, type, name, crs, northing, easting);
    }
}
