package com.is.bcs.adapter.out.persistence.controlpointimage;

import com.is.bcs.adapter.out.persistence.common.BaseCreatedTime;
import com.is.bcs.adapter.out.persistence.common.EntityReferences;
import com.is.bcs.adapter.out.persistence.controlpoint.ControlPointJpaEntity;
import com.is.bcs.adapter.out.persistence.member.MemberJpaEntity;
import com.is.bcs.adapter.out.persistence.survey.SurveyProjectJpaEntity;
import com.is.bcs.domain.controlpointimage.ControlPointImage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;

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

    /** 사진은 그 조사에 딸린 자료 — 프로젝트가 사라지면 함께 사라진다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_control_point_images_project"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SurveyProjectJpaEntity project;

    /** 찍은 대상 기준점 — 점이 사라지면 그 점의 사진도 함께 사라진다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "point_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_control_point_images_point"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ControlPointJpaEntity point;

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

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt;

    /** 올린 사람 — 회원이 지워져도 사진은 남고 이 칸만 비운다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", foreignKey = @ForeignKey(name = "fk_control_point_images_created_by"))
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private MemberJpaEntity createdBy;

    private ControlPointImageJpaEntity(
            Long id,
            SurveyProjectJpaEntity project,
            ControlPointJpaEntity point,
            String storedFileName,
            String storagePath,
            String originalFileName,
            String contentType,
            long fileSize,
            int width,
            int height,
            OffsetDateTime capturedAt,
            MemberJpaEntity createdBy
    ) {
        this.id = id;
        this.project = project;
        this.point = point;
        this.storedFileName = storedFileName;
        this.storagePath = storagePath;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.width = width;
        this.height = height;
        this.capturedAt = capturedAt;
        this.createdBy = createdBy;
    }

    public static ControlPointImageJpaEntity fromDomain(
            ControlPointImage image, EntityManager entityManager
    ) {
        return new ControlPointImageJpaEntity(
                image.getId(),
                EntityReferences.of(entityManager, SurveyProjectJpaEntity.class, image.getProjectId()),
                EntityReferences.of(entityManager, ControlPointJpaEntity.class, image.getPointId()),
                image.getStoredFileName(),
                image.getStoragePath(),
                image.getOriginalFileName(),
                image.getContentType(),
                image.getFileSize(),
                image.getWidth(),
                image.getHeight(),
                image.getCapturedAt(),
                EntityReferences.of(entityManager, MemberJpaEntity.class, image.getCreatedById())
        );
    }

    public ControlPointImage toDomain() {
        // 껍데기에서 id 만 읽는 접근이라 DB 에 가지 않는다
        return ControlPointImage.restore(
                id,
                project.getId(),
                point.getId(),
                storedFileName,
                storagePath,
                originalFileName,
                contentType,
                fileSize,
                width,
                height,
                createdBy == null ? null : createdBy.getId(),
                capturedAt,
                getCreatedAt()
        );
    }
}