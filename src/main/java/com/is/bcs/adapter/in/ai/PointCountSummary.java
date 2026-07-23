package com.is.bcs.adapter.in.ai;

import com.is.bcs.application.dto.ControlPointCountSummary;

import java.util.LinkedHashMap;
import java.util.Map;

/** 종류별 개수 요약(모델용) — countByType 키는 종류의 한글 표시명. */
public record PointCountSummary(long total, Map<String, Long> countByType) {

    public static PointCountSummary from(ControlPointCountSummary summary) {
        Map<String, Long> countByType = new LinkedHashMap<>();
        summary.countByType().forEach((type, count) -> countByType.put(type.getDisplayName(), count));
        return new PointCountSummary(summary.total(), countByType);
    }
}
