package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.adapter.out.persistence.common.BaseTime;
import com.is.bcs.adapter.out.persistence.common.EntityReferences;
import com.is.bcs.adapter.out.persistence.controlpoint.ControlPointJpaEntity;
import com.is.bcs.adapter.out.persistence.member.MemberJpaEntity;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_survey_records_project_point", columnNames = {"project_id", "point_id"})
        },
        indexes = {
                @Index(name = "idx_survey_records_project_id", columnList = "project_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyRecordJpaEntity extends BaseTime {

    @Id
    // IDENTITY 는 넣자마자 생성된 id 를 받아야 해서 INSERT 를 묶지 못한다 — 임포트가 수천 행을 한 번에 넣으므로 시퀀스로 미리 받는다
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "survey_records_seq")
    @SequenceGenerator(name = "survey_records_seq", sequenceName = "survey_records_seq", schema = "bcs", allocationSize = 50)
    private Long id;

    /** 기록은 프로젝트에 딸린 데이터 — 프로젝트가 사라지면 함께 사라진다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_survey_records_project"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SurveyProjectJpaEntity project;

    /** 조사한 기준점 — 기록이 걸려 있는 점은 지울 수 없다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "point_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_survey_records_point"))
    private ControlPointJpaEntity point;

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
            Long id, SurveyProjectJpaEntity project, ControlPointJpaEntity point,
            SurveyResult result, OffsetDateTime surveyedAt, String note, MemberJpaEntity surveyor
    ) {
        this.id = id;
        this.project = project;
        this.point = point;
        this.result = result;
        this.surveyedAt = surveyedAt;
        this.note = note;
        this.surveyor = surveyor;
    }

    public static SurveyRecordJpaEntity fromDomain(SurveyRecord record, EntityManager entityManager) {
        return new SurveyRecordJpaEntity(
                record.getId(),
                EntityReferences.of(entityManager, SurveyProjectJpaEntity.class, record.getProjectId()),
                EntityReferences.of(entityManager, ControlPointJpaEntity.class, record.getPointId()),
                record.getResult(), record.getSurveyedAt(), record.getNote(),
                EntityReferences.of(entityManager, MemberJpaEntity.class, record.getSurveyedById()));
    }

    public SurveyRecord toDomain() {
        // 껍데기에서 id 만 읽는 접근이라 DB 에 가지 않는다
        return SurveyRecord.restore(id, project.getId(), point.getId(), result, surveyedAt, note,
                surveyor == null ? null : surveyor.getId());
    }
}
