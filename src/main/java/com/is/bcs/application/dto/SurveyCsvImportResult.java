package com.is.bcs.application.dto;

public record SurveyCsvImportResult(
        Long projectId,
        int totalRows,
        int newPoints,
        int existingPoints,
        int updatedPoints,
        int createdRecords
) {
}
