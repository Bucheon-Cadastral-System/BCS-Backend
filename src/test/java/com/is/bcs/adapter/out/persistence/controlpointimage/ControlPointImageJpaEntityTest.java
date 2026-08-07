package com.is.bcs.adapter.out.persistence.controlpointimage;

import com.is.bcs.adapter.out.persistence.common.BaseCreatedTime;
import com.is.bcs.adapter.out.persistence.common.EntityReferenceStubs;
import com.is.bcs.domain.controlpointimage.ControlPointImage;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 도메인 ↔ JPA 엔티티 매핑 왕복 검증. */
class ControlPointImageJpaEntityTest {

    private final EntityManager entityManager = EntityReferenceStubs.entityManager();

    private static final OffsetDateTime CAPTURED_AT = OffsetDateTime.parse("2026-07-01T10:00:00+09:00");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-07-01T10:00:05+09:00");

    @Test
    @DisplayName("사진 왕복에서 프로젝트×기준점·파일 정보·촬영 시각이 보존된다")
    void imageRoundTrip_preservesAttributes() {
        ControlPointImage origin = ControlPointImage.restore(
                5L, 1L, 10L,
                "1465공_11111111-1111-1111-1111-111111111111.webp",
                "control-points/10/projects/1/1465공_11111111-1111-1111-1111-111111111111.webp",
                "IMG_0001.webp", "image/webp", 204_800L, 1920, 1080,
                9L, CAPTURED_AT, CREATED_AT);

        ControlPointImage restored = roundTrip(origin);

        assertEquals(5L, restored.getId());
        assertEquals(1L, restored.getProjectId());
        assertEquals(10L, restored.getPointId());
        assertEquals("1465공_11111111-1111-1111-1111-111111111111.webp", restored.getStoredFileName());
        assertEquals(
                "control-points/10/projects/1/1465공_11111111-1111-1111-1111-111111111111.webp",
                restored.getStoragePath());
        assertEquals("IMG_0001.webp", restored.getOriginalFileName());
        assertEquals("image/webp", restored.getContentType());
        assertEquals(204_800L, restored.getFileSize());
        assertEquals(1920, restored.getWidth());
        assertEquals(1080, restored.getHeight());
        assertEquals(9L, restored.getCreatedById());
        assertEquals(CAPTURED_AT, restored.getCapturedAt());
        assertEquals(CREATED_AT, restored.getCreatedAt());
    }

    @Test
    @DisplayName("올린 사람 없는 사진은 왕복 후에도 createdById가 null이다")
    void imageRoundTrip_withoutUploader_keepsNull() {
        ControlPointImage origin = ControlPointImage.restore(
                5L, 1L, 10L,
                "1465공_22222222-2222-2222-2222-222222222222.webp",
                "control-points/10/projects/1/1465공_22222222-2222-2222-2222-222222222222.webp",
                "IMG_0002.webp", "image/webp", 102_400L, 1280, 720,
                null, CAPTURED_AT, CREATED_AT);

        assertNull(roundTrip(origin).getCreatedById());
    }

    /**
     * createdAt은 JPA Auditing이 실제 저장 시점에 채우는 값이다.
     * 순수 매핑 테스트에는 영속성 컨텍스트가 없어 entityManager가 이 값을 대신 채워 주지 못하므로,
     * 왕복 검증이 보는 매핑 자체에 집중하기 위해 reflection으로 직접 채운다.
     */
    private ControlPointImage roundTrip(ControlPointImage origin) {
        ControlPointImageJpaEntity entity = ControlPointImageJpaEntity.fromDomain(origin, entityManager);
        setCreatedAt(entity, origin.getCreatedAt());
        return entity.toDomain();
    }

    private static void setCreatedAt(ControlPointImageJpaEntity entity, OffsetDateTime createdAt) {
        try {
            Field field = BaseCreatedTime.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(entity, createdAt);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
