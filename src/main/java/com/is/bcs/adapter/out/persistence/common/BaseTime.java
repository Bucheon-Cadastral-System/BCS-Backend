package com.is.bcs.adapter.out.persistence.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;

/**
 * 생성 + 수정 시각을 갖는 공통 상위 클래스 — 가변 엔티티용.
 * 수정이 없는 이력 엔티티가 updated_at을 물려받지 않도록 BaseCreatedTime과 분리했다.
 * deleted_at(soft delete)은 공통 계층에 두지 않고 필요한 엔티티에 개별 추가한다.
 */
@Getter
@MappedSuperclass
public abstract class BaseTime extends BaseCreatedTime {

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
