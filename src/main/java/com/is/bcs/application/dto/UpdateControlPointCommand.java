package com.is.bcs.application.dto;

import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.PointType;

import java.math.BigDecimal;

/**
 * 기준점 수정 — 식별(관리번호·이름·종류)과 성과만 다룬다.
 * 화면이 다루지 않는 소재지·설치·최종조사 항목은 요청에 싣지 않고 기존 값을 그대로 둔다.
 * 경위도는 받지 않는다 — 권위값(TM 성과)에서 서버가 파생한다.
 */
public record UpdateControlPointCommand(
        Long pointId,
        String pointNo,
        PointType type,
        String name,
        CoordinateSystem crs,
        BigDecimal northing,
        BigDecimal easting,
        /** 화면이 읽을 때 함께 받은 판 번호 — 그사이 누가 먼저 고쳤는지 가린다 */
        Long version
) {
}
