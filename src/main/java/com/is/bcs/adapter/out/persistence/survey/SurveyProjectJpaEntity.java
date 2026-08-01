package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.adapter.out.persistence.common.BaseTime;
import com.is.bcs.domain.survey.SurveyProject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "survey_projects", schema = "bcs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyProjectJpaEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 작성자(회원 id) — 인증이 붙기 전까지 비어 있어 nullable. */
    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // 원천이 날짜인 값이라 LocalDate — 시각으로 다루면 UTC 정규화 때 하루가 밀린다
    @Column(name = "started_on", nullable = false)
    private LocalDate startedOn;

    @Column(name = "ended_on")
    private LocalDate endedOn;

    @Column(name = "note")
    private String note;

    private SurveyProjectJpaEntity(
            Long id, Long authorId, String name, LocalDate startedOn, LocalDate endedOn, String note) {
        this.id = id;
        this.authorId = authorId;
        this.name = name;
        this.startedOn = startedOn;
        this.endedOn = endedOn;
        this.note = note;
    }

    public static SurveyProjectJpaEntity fromDomain(SurveyProject project) {
        return new SurveyProjectJpaEntity(
                project.getId(), project.getAuthorId(), project.getName(),
                project.getStartedOn(), project.getEndedOn(), project.getNote());
    }

    public SurveyProject toDomain() {
        return SurveyProject.restore(id, authorId, name, startedOn, endedOn, note);
    }
}
