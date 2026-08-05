package com.is.bcs.application.dto;

import com.is.bcs.domain.controlpoint.ControlPoint;

/**
 * 기준점 수정 결과.
 *
 * @param warning 부천 범위 밖 좌표 등 확인 요청 — 수정을 막지 않는다. 없으면 null
 */
public record UpdateControlPointResult(ControlPoint point, String warning) {
}
