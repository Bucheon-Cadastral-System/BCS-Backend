package com.is.bcs.application.dto;

public record ExcavationImportResult(
        Long projectId,
        int totalRows,
        int newPoints,
        int existingPoints,
        int createdRecords
) {
}
