package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.adapter.out.persistence.common.BaseTime;
import com.is.bcs.adapter.out.persistence.common.EntityReferences;
import com.is.bcs.adapter.out.persistence.member.MemberJpaEntity;
import com.is.bcs.domain.survey.SurveyProject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
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

    /** 작성자 — 인증이 붙기 전까지 비어 있어 nullable. 회원이 지워져도 프로젝트는 남고 이 칸만 비운다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", foreignKey = @ForeignKey(name = "fk_survey_projects_author"))
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private MemberJpaEntity author;

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
            Long id, MemberJpaEntity author, String name, LocalDate startedOn, LocalDate endedOn, String note) {
        this.id = id;
        this.author = author;
        this.name = name;
        this.startedOn = startedOn;
        this.endedOn = endedOn;
        this.note = note;
    }

    public static SurveyProjectJpaEntity fromDomain(SurveyProject project, EntityManager entityManager) {
        return new SurveyProjectJpaEntity(
                project.getId(), EntityReferences.of(entityManager, MemberJpaEntity.class, project.getAuthorId()),
                project.getName(), project.getStartedOn(), project.getEndedOn(), project.getNote());
    }

    public SurveyProject toDomain() {
        return SurveyProject.restore(id, author == null ? null : author.getId(), name, startedOn, endedOn, note);
    }
}
