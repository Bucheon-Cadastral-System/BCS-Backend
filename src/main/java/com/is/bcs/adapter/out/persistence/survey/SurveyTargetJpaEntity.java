package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.adapter.out.persistence.common.BaseTime;
import com.is.bcs.domain.survey.SurveyTarget;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "survey_targets",
        schema = "bcs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_survey_targets_project_point", columnNames = {"project_id", "point_id"})
        },
        indexes = {
                @Index(name = "idx_survey_targets_project_id", columnList = "project_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyTargetJpaEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "point_id", nullable = false)
    private Long pointId;

    private SurveyTargetJpaEntity(Long id, Long projectId, Long pointId) {
        this.id = id;
        this.projectId = projectId;
        this.pointId = pointId;
    }

    public static SurveyTargetJpaEntity fromDomain(SurveyTarget target) {
        return new SurveyTargetJpaEntity(target.getId(), target.getProjectId(), target.getPointId());
    }

    public SurveyTarget toDomain() {
        return SurveyTarget.restore(id, projectId, pointId);
    }
}
