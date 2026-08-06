package com.is.bcs.domain.controlpointimage;


import com.is.bcs.domain.controlpointimage.exception.InvalidControlPointImageException;
import lombok.Getter;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * 지적기준점 현장 사진.
 *
 * 사진은 조사 프로젝트와 기준점의 조합에 속한다.
 * 같은 (projectId, pointId) 조합에는 사진을 한 장만 둘 수 있으며,
 * 이 규칙은 영속 계층의 유니크 제약으로도 보장한다.
 *
 * 실제 이미지 데이터는 DB에 저장하지 않고 로컬 디스크에 저장한다.
 * storagePath에는 업로드 루트 디렉터리를 제외한 상대 경로만 저장한다.
 */
@Getter
public class ControlPointImage {

    private final Long id;
    private final Long projectId;
    private final Long pointId;

    private final String storedFileName;
    private final String storagePath;
    private final String originalFileName;
    private final String contentType;
    private final long fileSize;

    private final int width;
    private final int height;

    private final Long createdById;
    private final OffsetDateTime capturedAt;
    private final OffsetDateTime createdAt;

    private ControlPointImage(
            Long id,
            Long projectId,
            Long pointId,
            String storedFileName,
            String storagePath,
            String originalFileName,
            String contentType,
            long fileSize,
            int width,
            int height,
            Long createdById,
            OffsetDateTime capturedAt,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.projectId = Objects.requireNonNull(projectId, "조사 프로젝트 ID는 필수입니다.");
        this.pointId = Objects.requireNonNull(pointId, "기준점 ID는 필수입니다.");
        this.storedFileName = requireText(storedFileName, "저장 파일명");
        this.storagePath = requireText(storagePath, "저장 상대 경로");
        this.originalFileName = requireText(originalFileName, "원본 파일명");
        this.contentType = requireWebpContentType(contentType);
        this.fileSize = requirePositive(fileSize, "파일 크기");
        this.width = requirePositive(width, "이미지 가로 크기");
        this.height = requirePositive(height, "이미지 세로 크기");
        this.createdById = createdById;
        this.capturedAt = Objects.requireNonNull(capturedAt, "이미지 촬영 시각은 필수입니다.");
        this.createdAt = createdAt;
    }

    /**
     * 검증과 파일 저장이 완료된 새 이미지 정보 생성.
     *
     * createdAt은 JPA Auditing이 저장 시점에 생성하므로 신규 생성 시에는 null이다.
     */
    public static ControlPointImage create(
            Long projectId,
            Long pointId,
            String storedFileName,
            String storagePath,
            String originalFileName,
            String contentType,
            long fileSize,
            int width,
            int height,
            OffsetDateTime capturedAt,
            Long createdById
    ) {
        return new ControlPointImage(
                null,
                projectId,
                pointId,
                storedFileName,
                storagePath,
                originalFileName,
                contentType,
                fileSize,
                width,
                height,
                createdById,
                capturedAt,
                null
        );
    }

    /** DB 데이터를 도메인 객체로 복원한다. */
    public static ControlPointImage restore(
            Long id,
            Long projectId,
            Long pointId,
            String storedFileName,
            String storagePath,
            String originalFileName,
            String contentType,
            long fileSize,
            int width,
            int height,
            Long createdById,
            OffsetDateTime capturedAt,
            OffsetDateTime createdAt
    ) {
        return new ControlPointImage(
                Objects.requireNonNull(id, "이미지 ID는 필수입니다."),
                projectId,
                pointId,
                storedFileName,
                storagePath,
                originalFileName,
                contentType,
                fileSize,
                width,
                height,
                createdById,
                Objects.requireNonNull(capturedAt, "이미지 촬영 시각은 필수입니다."),
                Objects.requireNonNull(createdAt, "이미지 생성 시각은 필수입니다.")
        );
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidControlPointImageException(fieldName + "은(는) 필수입니다.");
        }
        return value.trim();
    }

    private static String requireWebpContentType(String contentType) {
        String value = requireText(contentType, "콘텐츠 타입");
        if (!"image/webp".equalsIgnoreCase(value)) {
            throw new InvalidControlPointImageException(
                    "WebP 이미지만 등록할 수 있습니다."
            );
        }
        return "image/webp";
    }

    private static long requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw new InvalidControlPointImageException(
                    fieldName + "은(는) 0보다 커야 합니다."
            );
        }
        return value;
    }

    private static int requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new InvalidControlPointImageException(
                    fieldName + "은(는) 0보다 커야 합니다."
            );
        }
        return value;
    }


}