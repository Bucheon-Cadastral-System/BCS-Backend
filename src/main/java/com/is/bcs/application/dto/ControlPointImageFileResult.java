package com.is.bcs.application.dto;

public record ControlPointImageFileResult(
        byte[] content,
        String contentType,
        long fileSize,
        String downloadFileName
) {
}