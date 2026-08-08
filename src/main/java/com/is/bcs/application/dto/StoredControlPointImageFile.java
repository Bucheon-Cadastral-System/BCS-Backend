package com.is.bcs.application.dto;

/**
 * 로컬 디스크 저장과 WebP 검증이 완료된 이미지 파일 정보.
 *
 * 실제 파일 내용은 포함하지 않고 DB에 기록할 메타데이터만 전달한다.
 */
public record StoredControlPointImageFile(
        String storedFileName,
        String storagePath,
        String originalFileName,
        String contentType,
        long fileSize,
        int width,
        int height
) {
}