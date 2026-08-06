package com.is.bcs.adapter.in.web.controlpointimage;

import com.is.bcs.adapter.in.security.CurrentMemberIdResolver;
import com.is.bcs.application.dto.UploadControlPointImageCommand;
import com.is.bcs.application.dto.UploadControlPointImageResult;
import com.is.bcs.application.port.in.controlpointimage.UploadControlPointImageUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/survey-projects")
public class ControlPointImageController {

    private final UploadControlPointImageUseCase uploadControlPointImageUseCase;
    private final CurrentMemberIdResolver currentMemberIdResolver;

    @PutMapping(
            path = "/{projectId}/control-points/{pointId}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ControlPointImageResponse> uploadOrReplace(
            @PathVariable("projectId") Long projectId,
            @PathVariable("pointId") Long pointId,
            @RequestPart("image") MultipartFile image,
            @RequestParam("capturedAt")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime capturedAt,
            Authentication authentication
    ) throws IOException {

        Long uploaderId = currentMemberIdResolver.resolve(authentication);

        UploadControlPointImageCommand command =
                new UploadControlPointImageCommand(
                        projectId,
                        pointId,
                        image.getOriginalFilename(),
                        image.getContentType(),
                        image.getSize(),
                        image.getBytes(),
                        capturedAt,
                        uploaderId
                );

        UploadControlPointImageResult result = uploadControlPointImageUseCase.uploadOrReplace(command);

        ControlPointImageResponse body = ControlPointImageResponse.from(result.image());

        if (!result.created()) {
            return ResponseEntity.ok(body);
        }

        URI location = URI.create("/api/control-point-images/%d/file".formatted(result.image().getId()));

        return ResponseEntity
                .created(location)
                .body(body);
    }
}