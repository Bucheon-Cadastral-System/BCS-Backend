package com.is.bcs.adapter.in.web.controlpoint;

import com.is.bcs.application.dto.UpdateControlPointResult;

/**
 * 기준점 수정 응답.
 *
 * @param warning 부천 범위 밖 좌표 등 확인 요청 — 수정을 막지 않는다. 없으면 null
 */
public record UpdateControlPointResponse(ControlPointResponse point, String warning) {

    public static UpdateControlPointResponse from(UpdateControlPointResult result) {
        return new UpdateControlPointResponse(ControlPointResponse.from(result.point()), result.warning());
    }
}
