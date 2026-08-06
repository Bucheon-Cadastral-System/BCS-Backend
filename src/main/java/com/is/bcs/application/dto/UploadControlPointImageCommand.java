package com.is.bcs.application.dto;

/**
 * 기준점 현장 이미지 업로드 명령.
 *
 * pointName과 작성자 ID는 요청 본문에서 받지 않는다.
 * 기준점명은 DB에서, 작성자 ID는 인증 주체에서 가져온다.
 */
public record UploadControlPointImageCommand(
        Long projectId,
        Long pointId,
        String originalFileName,
        String contentType,
        long fileSize,
        byte[] content,
        Long uploaderId
) {
}