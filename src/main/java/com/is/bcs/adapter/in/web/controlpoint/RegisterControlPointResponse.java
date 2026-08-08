package com.is.bcs.adapter.in.web.controlpoint;

import com.is.bcs.application.dto.RegisterControlPointResult;

/**
 * 기준점 등록 응답 — 임포트와 같은 규칙이라 신규(201)만이 아니라 기존 점 갱신·재사용(200)으로도 끝난다.
 *
 * @param created 새 점을 만들었는지
 * @param updated 같은 이름·종류의 기존 점을 입력 값으로 덮었는지
 * @param warning 부천 범위 밖 좌표 등 확인 요청 — 등록을 막지 않는다. 없으면 null
 */
public record RegisterControlPointResponse(
        ControlPointResponse point, boolean created, boolean updated, String warning) {

    public static RegisterControlPointResponse from(RegisterControlPointResult result) {
        return new RegisterControlPointResponse(
                ControlPointResponse.from(result.point()), result.created(), result.updated(), result.warning());
    }
}
