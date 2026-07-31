package com.is.bcs.application.dto;

import java.util.List;
import java.util.Map;

/**
 * 대상지 파일을 등록하지 않고 읽어만 본 결과.
 * 담당자가 확정 전에 "몇 건인지 · 어떤 열로 읽혔는지 · 어느 행이 잘못됐는지"를 확인할 수 있게 한다.
 *
 * @param totalRows        표의 데이터 행 수 (읽힌 행 + 오류 행)
 * @param recognizedColumns 파일의 열 이름 → 읽어 들인 항목. 파일에 적힌 순서를 유지한다.
 * @param ignoredColumns   알아보지 못해 버린 열 — 조용히 빠지지 않도록 함께 알린다.
 */
public record SurveyCsvPreviewResult(
        int totalRows,
        Map<String, String> recognizedColumns,
        List<String> ignoredColumns,
        List<RowError> errors
) {

    public record RowError(int row, String message) {
    }
}
