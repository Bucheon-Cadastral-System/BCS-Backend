package com.is.bcs.application.dto;

import com.is.bcs.domain.controlpoint.PointType;

import java.util.Map;

/** 기준점 개수 요약 — countByType은 모든 종류를 키로 갖는다(없는 종류는 0). */
public record ControlPointCountSummary(long total, Map<PointType, Long> countByType) {
}
