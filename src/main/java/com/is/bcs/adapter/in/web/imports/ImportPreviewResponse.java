package com.is.bcs.adapter.in.web.imports;

import com.is.bcs.application.dto.ImportPreviewResult;

import java.util.List;
import java.util.Map;

/**
 * 대상지 파일 미리보기 응답 — 등록하지 않고 읽어만 본 결과.
 *
 */
public record ImportPreviewResponse(
        int totalRows,
        List<RowError> errors,
        List<RowWarning> warnings,
        List<String> missingColumns,
        List<String> foreignColumns
) {

    public record RowError(int row, String message) {
    }

    /** 등록을 막지는 않지만 담당자가 확인해야 할 행. */
    public record RowWarning(int row, String message) {
    }

    public static ImportPreviewResponse from(ImportPreviewResult result) {
        return new ImportPreviewResponse(
                result.totalRows(),
                result.errors().stream().map(e -> new RowError(e.row(), e.message())).toList(),
                result.warnings().stream().map(w -> new RowWarning(w.row(), w.message())).toList(),
                result.missingColumns(),
                result.foreignColumns());
    }
}
