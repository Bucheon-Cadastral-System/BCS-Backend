package com.is.bcs.adapter.in.web.imports;

import com.is.bcs.application.dto.SurveyCsvPreviewResult;

import java.util.List;
import java.util.Map;

/**
 * 대상지 파일 미리보기 응답 — 등록하지 않고 읽어만 본 결과.
 *
 * @param recognizedColumns 파일의 열 이름 → 읽어 들인 항목
 * @param extraColumns      해석하지 않고 값만 보관하는 열
 */
public record SurveyCsvPreviewResponse(
        int totalRows,
        Map<String, String> recognizedColumns,
        List<String> extraColumns,
        List<RowError> errors
) {

    public record RowError(int row, String message) {
    }

    public static SurveyCsvPreviewResponse from(SurveyCsvPreviewResult result) {
        return new SurveyCsvPreviewResponse(
                result.totalRows(),
                result.recognizedColumns(),
                result.extraColumns(),
                result.errors().stream().map(e -> new RowError(e.row(), e.message())).toList());
    }
}
