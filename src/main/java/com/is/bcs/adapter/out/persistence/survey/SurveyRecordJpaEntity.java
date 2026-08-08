package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.adapter.out.persistence.common.BaseTime;
import com.is.bcs.adapter.out.persistence.common.EntityReferences;
import com.is.bcs.adapter.out.persistence.member.MemberJpaEntity;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(
        name = "survey_records",
        schema = "bcs",
        // 기본키가 (point_id, project_id) 순서로 서므로 프로젝트로 좁히는 조회는 이 인덱스를 쓴다
        indexes = @Index(name = "idx_survey_records_project_id", columnList = "project_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyRecordJpaEntity extends BaseTime {

    /**
     * 기록의 식별자는 그것이 답하는 대상, 곧 (프로젝트, 기준점)이다.
     *
     * <p>한 대상에 기록은 하나다. 정정은 새 행이 아니라 같은 행의 교체이므로 대리키를 둘 자리가 없다.
     * 새 기록은 이 값을 비운 채로 만든다. {@code @MapsId} 가 대상 참조에서 채우고, 그때까지 비어 있어야
     * 스프링 데이터가 새 행으로 보고 저장 경로에서 SELECT 를 한 번 더 내지 않는다.
     */
    @EmbeddedId
    private ProjectPointId id;

    /**
     * 이 기록이 답하는 조사 대상 — 대상이 아닌 점에는 기록을 남길 수 없다.
     *
     * <p>대상을 거치므로 프로젝트·기준점 외래키를 따로 걸지 않는다. 두 열이 이미 대상 행을 가리키고,
     * 대상 행이 프로젝트와 기준점의 실재를 보증한다. 프로젝트가 사라지면 대상이 사라지고 기록도 함께 사라진다.
     *
     * <p>한 대상에 기록이 하나뿐이라는 사실은 기본키가 지킨다. 매핑은 참조 방향만 말하면 되므로
     * 일대일이 아니라 다대일로 둔다.
     */
    @MapsId
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns(
            value = {
                    @JoinColumn(name = "project_id", referencedColumnName = "project_id", nullable = false),
                    @JoinColumn(name = "point_id", referencedColumnName = "point_id", nullable = false)
            },
            foreignKey = @ForeignKey(name = "fk_survey_records_target"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SurveyTargetJpaEntity target;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private SurveyResult result;

    @Column(name = "surveyed_at", nullable = false)
    private OffsetDateTime surveyedAt;

    @Column(name = "note")
    private String note;

    /** 마지막으로 판정한 조사원 — 회원이 지워져도 기록은 남고 이 칸만 비운다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "surveyed_by", foreignKey = @ForeignKey(name = "fk_survey_records_surveyed_by"))
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private MemberJpaEntity surveyor;

    private SurveyRecordJpaEntity(
            SurveyTargetJpaEntity target,
            SurveyResult result, OffsetDateTime surveyedAt, String note, MemberJpaEntity surveyor
    ) {
        this.target = target;
        this.result = result;
        this.surveyedAt = surveyedAt;
        this.note = note;
        this.surveyor = surveyor;
    }

    public static SurveyRecordJpaEntity fromDomain(SurveyRecord record, EntityManager entityManager) {
        return new SurveyRecordJpaEntity(
                EntityReferences.of(entityManager, SurveyTargetJpaEntity.class,
                        new ProjectPointId(record.getProjectId(), record.getPointId())),
                record.getResult(), record.getSurveyedAt(), record.getNote(),
                EntityReferences.of(entityManager, MemberJpaEntity.class, record.getSurveyedById()));
    }

    public SurveyRecord toDomain() {
        // 껍데기에서 id 만 읽는 접근이라 DB 에 가지 않는다
        ProjectPointId key = target.getId();
        return SurveyRecord.restore(key.getProjectId(), key.getPointId(), result, surveyedAt, note,
                surveyor == null ? null : surveyor.getId());
    }
}
