package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.adapter.out.persistence.common.BaseTime;
import com.is.bcs.domain.survey.SurveyTarget;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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
    // IDENTITY 는 넣자마자 생성된 id 를 받아야 해서 INSERT 를 묶지 못한다 — 임포트가 수천 행을 한 번에 넣으므로 시퀀스로 미리 받는다
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "survey_targets_seq")
    @SequenceGenerator(name = "survey_targets_seq", sequenceName = "survey_targets_seq", schema = "bcs", allocationSize = 50)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "point_id", nullable = false)
    private Long pointId;

    /**
     * 기본 양식에 없어 해석하지 않은 열 — 이름을 컬럼으로 만들지 않고 행으로 쌓는다.
     * 고객사가 열을 더할 때마다 스키마를 고치지 않아도 되고, 파일에 적힌 순서도 그대로 남는다.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "survey_target_extras",
            schema = "bcs",
            joinColumns = @JoinColumn(name = "target_id"),
            indexes = @Index(name = "idx_survey_target_extras_target_id", columnList = "target_id")
    )
    @OrderColumn(name = "position")
    private List<ExtraColumnEmbeddable> extras;

    private SurveyTargetJpaEntity(Long id, Long projectId, Long pointId, List<ExtraColumnEmbeddable> extras) {
        this.id = id;
        this.projectId = projectId;
        this.pointId = pointId;
        this.extras = extras;
    }

    public static SurveyTargetJpaEntity fromDomain(SurveyTarget target) {
        return new SurveyTargetJpaEntity(
                target.getId(), target.getProjectId(), target.getPointId(),
                target.getExtras().stream().map(ExtraColumnEmbeddable::fromDomain).toList());
    }

    public SurveyTarget toDomain() {
        return SurveyTarget.restore(id, projectId, pointId,
                extras == null ? List.of() : extras.stream().map(ExtraColumnEmbeddable::toDomain).toList());
    }
}
