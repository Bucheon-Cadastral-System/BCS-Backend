package com.is.bcs.adapter.out.persistence.controlpointimage;

import com.is.bcs.adapter.out.persistence.common.BaseCreatedTime;
import com.is.bcs.domain.controlpointimage.ControlPointImage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "control_point_images",
        schema = "bcs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_control_point_images_project_point",
                        columnNames = {"project_id", "point_id"}
                )
        },
        indexes = {
                @Index(name = "idx_control_point_images_point_id", columnList = "point_id"),
                @Index(name = "idx_control_point_images_project_id", columnList = "project_id"),
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ControlPointImageJpaEntity extends BaseCreatedTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "point_id", nullable = false)
    private Long pointId;

    @Column(name = "stored_file_name", nullable = false, length = 150)
    private String storedFileName;

    /** 업로드 루트 디렉터리를 제외한 상대 경로.
     *  예: control-points/42/projects/7/파일명.webp */
    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "width", nullable = false)
    private int width;

    @Column(name = "height", nullable = false)
    private int height;

    @Column(name = "created_by")
    private Long createdById;

    private ControlPointImageJpaEntity(
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
            Long createdById
    ) {
        this.id = id;
        this.projectId = projectId;
        this.pointId = pointId;
        this.storedFileName = storedFileName;
        this.storagePath = storagePath;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.width = width;
        this.height = height;
        this.createdById = createdById;
    }

    public static ControlPointImageJpaEntity fromDomain(
            ControlPointImage image
    ) {
        return new ControlPointImageJpaEntity(
                image.getId(),
                image.getProjectId(),
                image.getPointId(),
                image.getStoredFileName(),
                image.getStoragePath(),
                image.getOriginalFileName(),
                image.getContentType(),
                image.getFileSize(),
                image.getWidth(),
                image.getHeight(),
                image.getCreatedById()
        );
    }

    public ControlPointImage toDomain() {
        return ControlPointImage.restore(
                id,
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
                getCreatedAt()
        );
    }
}