package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.adapter.out.persistence.common.BaseTime;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyProjectType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "survey_projects", schema = "bcs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyProjectJpaEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private SurveyProjectType type;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "note")
    private String note;

    private SurveyProjectJpaEntity(Long id, SurveyProjectType type, String name, String note) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.note = note;
    }

    public static SurveyProjectJpaEntity fromDomain(SurveyProject project) {
        return new SurveyProjectJpaEntity(
                project.getId(), project.getType(), project.getName(), project.getNote());
    }

    public SurveyProject toDomain() {
        return SurveyProject.restore(id, type, name, note);
    }
}
