package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.adapter.out.persistence.common.BaseTime;
import com.is.bcs.adapter.out.persistence.common.EntityReferences;
import com.is.bcs.adapter.out.persistence.controlpoint.ControlPointJpaEntity;
import com.is.bcs.domain.survey.SurveyTarget;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;

@Getter
@Entity
@Table(
        name = "survey_targets",
        schema = "bcs",
        // 기본키가 (point_id, project_id) 순서로 서므로 프로젝트로 좁히는 조회는 이 인덱스를 쓴다
        indexes = @Index(name = "idx_survey_targets_project_id", columnList = "project_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyTargetJpaEntity extends BaseTime {

    /**
     * (프로젝트, 기준점) 자연키가 곧 식별자다.
     *
     * <p>새 대상은 이 값을 비운 채로 만든다. {@code @MapsId} 가 연관에서 채우고, 그때까지 id 가 비어 있어야
     * 스프링 데이터가 새 행으로 보고 저장 경로에서 SELECT 를 한 번 더 내지 않는다.
     */
    @EmbeddedId
    private ProjectPointId id;

    /** 대상은 프로젝트에 딸린 데이터 — 프로젝트가 사라지면 함께 사라진다. */
    @MapsId("projectId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_survey_targets_project"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SurveyProjectJpaEntity project;

    /** 조사 대상 기준점 — 조사가 걸려 있는 점은 지울 수 없다. */
    @MapsId("pointId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "point_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_survey_targets_point"))
    private ControlPointJpaEntity point;

    /**
     * 기본 양식에 없어 해석하지 않은 열 — 이름을 컬럼으로 만들지 않고 행으로 쌓는다.
     * 고객사가 열을 더할 때마다 스키마를 고치지 않아도 되고, 파일에 적힌 순서도 그대로 남는다.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "survey_target_extras",
            schema = "bcs",
            joinColumns = {
                    @JoinColumn(name = "project_id", referencedColumnName = "project_id"),
                    @JoinColumn(name = "point_id", referencedColumnName = "point_id")
            },
            foreignKey = @ForeignKey(name = "fk_survey_target_extras_target"),
            indexes = @Index(name = "idx_survey_target_extras_target", columnList = "project_id, point_id")
    )
    @OrderColumn(name = "position")
    private List<ExtraColumnEmbeddable> extras;

    private SurveyTargetJpaEntity(
            SurveyProjectJpaEntity project, ControlPointJpaEntity point, List<ExtraColumnEmbeddable> extras) {
        this.project = project;
        this.point = point;
        this.extras = extras;
    }

    public static SurveyTargetJpaEntity fromDomain(SurveyTarget target, EntityManager entityManager) {
        return new SurveyTargetJpaEntity(
                EntityReferences.of(entityManager, SurveyProjectJpaEntity.class, target.getProjectId()),
                EntityReferences.of(entityManager, ControlPointJpaEntity.class, target.getPointId()),
                target.getExtras().stream().map(ExtraColumnEmbeddable::fromDomain).toList());
    }

    public SurveyTarget toDomain() {
        // 껍데기에서 id 만 읽는 접근이라 DB 에 가지 않는다
        return SurveyTarget.restore(project.getId(), point.getId(),
                extras == null ? List.of() : extras.stream().map(ExtraColumnEmbeddable::toDomain).toList());
    }
}
