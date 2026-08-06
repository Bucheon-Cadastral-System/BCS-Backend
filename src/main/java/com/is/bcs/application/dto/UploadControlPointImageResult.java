package com.is.bcs.application.dto;

import com.is.bcs.domain.controlpointimage.ControlPointImage;

public record UploadControlPointImageResult(
        ControlPointImage image,
        boolean created
) {
}