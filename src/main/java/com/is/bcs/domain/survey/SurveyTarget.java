package com.is.bcs.domain.survey;

import lombok.Getter;

import java.util.Objects;

/**
 * 조사 대상 — 조사 프로젝트가 조사 책임지는 기준점. (프로젝트, 기준점) 쌍은 하나만 존재한다(영속 계층 유니크).
 * 조사기록(SurveyRecord)과 분리한다: 대상이지만 아직 조사하지 않은 점(미조사)을 표현하려면 조사됨 축과 별개의 대상 집합이 필요하다.
 * 진행률의 분모(전체 대상 수)는 이 대상 집합으로 세고, 분자(조사됨)는 조사기록으로 센다.
 */
@Getter
public class SurveyTarget {

    private final Long id;
    private final Long projectId;
    private final Long pointId;

    private SurveyTarget(Long id, Long projectId, Long pointId) {
        this.id = id;
        this.projectId = Objects.requireNonNull(projectId, "프로젝트 ID는 필수입니다.");
        this.pointId = Objects.requireNonNull(pointId, "기준점 ID는 필수입니다.");
    }

    public static SurveyTarget create(Long projectId, Long pointId) {
        return new SurveyTarget(null, projectId, pointId);
    }

    /** DB 데이터를 도메인 객체로 복원한다. */
    public static SurveyTarget restore(Long id, Long projectId, Long pointId) {
        return new SurveyTarget(id, projectId, pointId);
    }
}
