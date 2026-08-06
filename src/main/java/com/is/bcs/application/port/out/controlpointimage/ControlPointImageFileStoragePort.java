package com.is.bcs.application.port.out.controlpointimage;

import com.is.bcs.application.dto.StoredControlPointImageFile;

import java.time.OffsetDateTime;

public interface ControlPointImageFileStoragePort {

    StoredControlPointImageFile store(
            Long projectId,
            Long pointId,
            String pointName,
            OffsetDateTime capturedAt,
            String originalFileName,
            String contentType,
            long declaredFileSize,
            byte[] content
    );

    /**
     * 파일이 이미 없어도 정상 처리한다.
     * DB 롤백 정리와 커밋 후 이전 파일 정리에 사용한다.
     */
    void deleteIfExists(String storagePath);
}