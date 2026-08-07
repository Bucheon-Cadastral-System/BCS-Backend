package com.is.bcs.adapter.out.persistence.survey;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * 조사 대상의 식별자 — (프로젝트, 기준점) 쌍이다.
 *
 * <p>대리키를 두지 않는다. 이 쌍은 원래 유니크 제약으로 지키던 자연키였고, 그 위에 시퀀스 번호를 하나 더 얹으면
 * 같은 사실을 두 곳에서 지키게 된다. 기본키로 올리면 제약이 하나로 줄고, 기록에서 대상으로 가는 외래키를
 * 편법 없이 걸 수 있다.
 *
 * <p>레코드가 아니라 클래스인 것은 {@code @MapsId} 때문이다. 하이버네이트가 연관에서 읽은 id 를 이 객체의
 * 필드에 채워 넣으므로 값을 바꿀 수 있어야 한다.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectPointId implements Serializable {

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "point_id", nullable = false)
    private Long pointId;

    public ProjectPointId(Long projectId, Long pointId) {
        this.projectId = projectId;
        this.pointId = pointId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ProjectPointId that)) return false;
        return Objects.equals(projectId, that.projectId) && Objects.equals(pointId, that.pointId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, pointId);
    }
}
