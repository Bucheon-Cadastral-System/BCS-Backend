package com.is.bcs.application.dto;

public record ImportExcavationCsvCommand(
        String name,
        String note,
        byte[] content
) {
}
