package com.is.bcs.adapter.in.web.controlpointimage;

import com.is.bcs.adapter.in.security.CurrentMemberIdResolver;
import com.is.bcs.application.dto.ControlPointImageFileResult;
import com.is.bcs.application.port.in.controlpointimage.GetControlPointImageFileUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/control-point-images")
public class ControlPointImageFileController {

    private final GetControlPointImageFileUseCase getControlPointImageFileUseCase;
    private final CurrentMemberIdResolver currentMemberIdResolver;

    @GetMapping("/{imageId}/file")
    public ResponseEntity<byte[]> view(@PathVariable("imageId") Long imageId, Authentication authentication) {
        ControlPointImageFileResult result = getFile(imageId, authentication);

        ContentDisposition contentDisposition =
                ContentDisposition.inline()
                        .filename(
                                result.downloadFileName(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        return fileResponse(result, contentDisposition);
    }

    @GetMapping("/{imageId}/download")
    public ResponseEntity<byte[]> download(@PathVariable("imageId") Long imageId, Authentication authentication) {
        ControlPointImageFileResult result = getFile(imageId, authentication);

        ContentDisposition contentDisposition =
                ContentDisposition.attachment()
                        .filename(result.downloadFileName(), StandardCharsets.UTF_8)
                        .build();

        return fileResponse(result, contentDisposition);
    }

    private ControlPointImageFileResult getFile(Long imageId, Authentication authentication) {
        Long requesterId = currentMemberIdResolver.resolve(authentication);

        return getControlPointImageFileUseCase.getFile(imageId, requesterId);
    }

    private static ResponseEntity<byte[]> fileResponse(ControlPointImageFileResult result, ContentDisposition contentDisposition) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.fileSize())
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition.toString()
                )
                .body(result.content());
    }
}