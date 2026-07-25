package com.is.bcs.adapter.in.web.imports;

import com.is.bcs.application.dto.SurveyCsvImportResult;

public record SurveyCsvImportResponse(
        Long projectId,
        int totalRows,
        int newPoints,
        int existingPoints,
        int updatedPoints,
        int createdRecords
) {

    public static SurveyCsvImportResponse from(SurveyCsvImportResult result) {
        return new SurveyCsvImportResponse(
                result.projectId(), result.totalRows(),
                result.newPoints(), result.existingPoints(), result.updatedPoints(), result.createdRecords());
    }
}
