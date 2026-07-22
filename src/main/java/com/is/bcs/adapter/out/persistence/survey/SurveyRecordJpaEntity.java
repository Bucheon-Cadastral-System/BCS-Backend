package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.adapter.out.persistence.common.BaseTime;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "point_id", nullable = false)
    private Long pointId;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private SurveyResult result;

    @Column(name = "surveyed_at", nullable = false)
    private OffsetDateTime surveyedAt;

    @Column(name = "note")
    private String note;

    private SurveyRecordJpaEntity(
            Long id, Long projectId, Long pointId,
            SurveyResult result, OffsetDateTime surveyedAt, String note
    ) {
        this.id = id;
        this.projectId = projectId;
        this.pointId = pointId;
        this.result = result;
        this.surveyedAt = surveyedAt;
        this.note = note;
    }

    public static SurveyRecordJpaEntity fromDomain(SurveyRecord record) {
        return new SurveyRecordJpaEntity(
                record.getId(), record.getProjectId(), record.getPointId(),
                record.getResult(), record.getSurveyedAt(), record.getNote());
    }

    public SurveyRecord toDomain() {
        return SurveyRecord.restore(id, projectId, pointId, result, surveyedAt, note);
    }
}
