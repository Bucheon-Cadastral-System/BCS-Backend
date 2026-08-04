package com.is.bcs.adapter.in.web.imports;

import com.is.bcs.application.dto.ControlPointImportResult;

public record ControlPointImportResponse(
        int totalRows,
        int newPoints,
        int existingPoints,
        int updatedPoints
) {

    public static ControlPointImportResponse from(ControlPointImportResult result) {
        return new ControlPointImportResponse(
                result.totalRows(), result.newPoints(), result.existingPoints(), result.updatedPoints());
    }
}
