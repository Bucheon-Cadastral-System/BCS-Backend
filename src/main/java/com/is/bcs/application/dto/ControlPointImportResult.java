package com.is.bcs.application.dto;

/** 기준점 파일 등록 결과 — 기존=성과가 같아 그대로 둔 점, 갱신=파일 값으로 덮은 점. */
public record ControlPointImportResult(int totalRows, int newPoints, int existingPoints, int updatedPoints) {
}
