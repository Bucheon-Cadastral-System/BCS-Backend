package com.is.bcs.adapter.in.member;

import com.is.bcs.adapter.in.security.CurrentMemberIdResolver;
import com.is.bcs.application.port.in.member.DeleteMemberProfileImageUseCase;
import com.is.bcs.application.port.in.member.GetMemberProfileImageUseCase;
import com.is.bcs.application.port.in.member.UploadMemberProfileImageUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberProfileImageController {

    private final UploadMemberProfileImageUseCase uploadUseCase;
    private final DeleteMemberProfileImageUseCase deleteUseCase;
    private final GetMemberProfileImageUseCase getUseCase;
    private final CurrentMemberIdResolver currentMemberIdResolver;

    @PutMapping(path = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadOrReplace(
            @RequestPart("image") MultipartFile image,
            Authentication authentication
    ) throws IOException {

        Long memberId = currentMemberIdResolver.resolve(authentication);

        uploadUseCase.uploadOrReplace(
                new UploadMemberProfileImageUseCase.Command(
                        memberId,
                        image.getContentType(),
                        image.getSize(),
                        image.getBytes()
                )
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/profile-image")
    public ResponseEntity<Void> delete(Authentication authentication) {
        Long memberId = currentMemberIdResolver.resolve(authentication);

        deleteUseCase.delete(memberId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{memberId}/profile-image", produces = "image/webp")
    public ResponseEntity<byte[]> get(@PathVariable("memberId") Long memberId, Authentication authentication) {
        Long requesterId = currentMemberIdResolver.resolve(authentication);

        GetMemberProfileImageUseCase.Result result =
                getUseCase.getFile(
                        memberId,
                        requesterId
                );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.fileSize())
                .cacheControl(CacheControl.noStore())
                .body(result.content());
    }
}