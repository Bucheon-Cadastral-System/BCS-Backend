package com.is.bcs.adapter.in.web.imports;

import com.is.bcs.application.dto.ExcavationImportResult;

public record ExcavationImportResponse(
        Long projectId,
        int totalRows,
        int newPoints,
        int existingPoints,
        int updatedPoints,
        int createdRecords
) {

    public static ExcavationImportResponse from(ExcavationImportResult result) {
        return new ExcavationImportResponse(
                result.projectId(), result.totalRows(),
                result.newPoints(), result.existingPoints(), result.updatedPoints(), result.createdRecords());
    }
}
