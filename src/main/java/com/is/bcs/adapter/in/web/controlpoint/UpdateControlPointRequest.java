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
 *
 * <p>판 번호는 조회 응답에서 받은 값을 그대로 돌려보낸다. 수정 창을 열어 둔 사이 다른 사람이 먼저 저장했으면
 * 그 번호가 올라 있어 서버가 거절한다. 사람이 폼을 채우는 시간은 두 요청 사이라 행 잠금으로는 덮을 수 없다.
 */
public record UpdateControlPointRequest(
        @NotBlank(message = "관리번호는 필수입니다.") String pointNo,
        @NotNull(message = "종류는 필수입니다.") PointType type,
        @NotBlank(message = "기준점명은 필수입니다.") String name,
        @NotNull(message = "좌표계는 필수입니다.") CoordinateSystem crs,
        @NotNull(message = "북방향(X) 성과는 필수입니다.") BigDecimal northing,
        @NotNull(message = "동방향(Y) 성과는 필수입니다.") BigDecimal easting,
        @NotNull(message = "판 번호는 필수입니다.") Long version
) {

    public UpdateControlPointCommand toCommand(Long pointId) {
        return new UpdateControlPointCommand(pointId, pointNo, type, name, crs, northing, easting, version);
    }
}
