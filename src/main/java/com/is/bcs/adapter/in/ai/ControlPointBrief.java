package com.is.bcs.adapter.in.ai;

import com.is.bcs.domain.controlpoint.ControlPoint;

/** 기준점 찾기 결과 한 줄(모델용) — 어느 점인지 가리는 데 필요한 값만 담는다. 좌표는 상세 조회가 준다. */
public record ControlPointBrief(String pointNo, String name, String type, String regionName, String address) {

    public static ControlPointBrief from(ControlPoint point) {
        return new ControlPointBrief(
                point.getPointNo(), point.getName(), point.getType().getDisplayName(),
                point.getRegionName(), point.getAddress());
    }
}
