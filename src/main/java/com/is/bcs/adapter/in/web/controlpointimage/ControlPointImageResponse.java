package com.is.bcs.adapter.in.web.controlpointimage;

import com.is.bcs.domain.controlpointimage.ControlPointImage;

import java.time.OffsetDateTime;

public record ControlPointImageResponse(
        Long id,
        Long projectId,
        Long controlPointId,
        String url,
        String originalFileName,
        long size,
        int width,
        int height,
        Long createdById,
        OffsetDateTime capturedAt,
        OffsetDateTime createdAt
) {

    public static ControlPointImageResponse from(ControlPointImage image) {
        return new ControlPointImageResponse(
                image.getId(),
                image.getProjectId(),
                image.getPointId(),
                "/api/control-point-images/%d/file".formatted(image.getId()),
                image.getOriginalFileName(),
                image.getFileSize(),
                image.getWidth(),
                image.getHeight(),
                image.getCreatedById(),
                image.getCapturedAt(),
                image.getCreatedAt()
        );
    }

}