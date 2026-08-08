package com.is.bcs.adapter.in.web.controlpointimage;

import com.is.bcs.adapter.in.security.CurrentMemberIdResolver;
import com.is.bcs.application.dto.UploadControlPointImageCommand;
import com.is.bcs.application.dto.UploadControlPointImageResult;
import com.is.bcs.application.port.in.controlpointimage.UploadControlPointImageUseCase;
import com.is.bcs.domain.survey.SurveyResult;
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

    /**
     * 현장 사진과 그 자리에서 내린 판정을 함께 받는다.
     *
     * <p>둘을 따로 받으면 한쪽만 성공하는 상태가 생긴다. 그때 사용자에게 남는 선택지는
     * "사진은 올라갔으니 판정만 다시 하세요" 뿐인데, 무엇이 어디까지 됐는지 화면으로는 알 수 없다.
     * 한 요청 한 트랜잭션으로 묶어 되든 안 되든 통째로 되게 한다.
     *
     * @param result 필수 — 사진만 보고 서버가 정할 수 없다. 판정 없이 사진만 남기는 길은 두지 않는다.
     * @param note   기타를 골랐을 때의 사유. 다른 갈래에서는 비운다.
     */
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
            @RequestParam("result") SurveyResult result,
            @RequestParam(value = "note", required = false) String note,
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
                        result,
                        note,
                        uploaderId
                );

        UploadControlPointImageResult uploaded = uploadControlPointImageUseCase.uploadOrReplace(command);

        ControlPointImageResponse body = ControlPointImageResponse.from(uploaded.image());

        if (!uploaded.created()) {
            return ResponseEntity.ok(body);
        }

        URI location = URI.create("/api/control-point-images/%d/file".formatted(uploaded.image().getId()));

        return ResponseEntity.created(location).body(body);
    }
}
