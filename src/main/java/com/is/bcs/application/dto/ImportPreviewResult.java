package com.is.bcs.application.dto;

import java.util.List;
import java.util.Map;

/**
 * 대상지 파일을 등록하지 않고 읽어만 본 결과.
 * 담당자가 확정 전에 "몇 건인지 · 어떤 열로 읽혔는지 · 어느 행이 잘못됐는지"를 확인할 수 있게 한다.
 *
 * @param totalRows        표의 데이터 행 수 (읽힌 행 + 오류 행)
 * @param recognizedColumns 파일의 열 이름 → 읽어 들인 항목. 파일에 적힌 순서를 유지한다.
 * @param extraColumns     해석하지 않고 값만 보관하는 열 — 버리는 것이 아니라 그대로 저장된다.
 * @param errors           읽지 못한 행 — 하나라도 있으면 등록이 거부된다.
 * @param warnings         읽히기는 했으나 확인이 필요한 행 — 등록을 막지 않는다.
 * @param missingColumns   이 서식에 있을 것으로 본 열 중 빠진 것 — 등록을 막지 않고 확인만 요청한다.
 * @param foreignColumns   다른 서식에만 있는 열 중 이 파일에 있는 것 — 마찬가지로 확인만 요청한다.
 */
public record ImportPreviewResult(
        int totalRows,
        Map<String, String> recognizedColumns,
        List<String> extraColumns,
        List<RowError> errors,
        List<RowWarning> warnings,
        List<String> missingColumns,
        List<String> foreignColumns
) {

    public record RowError(int row, String message) {
    }

    public record RowWarning(int row, String message) {
    }
}
