package com.is.bcs.domain.survey;

import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * 조사기록 — 조사 프로젝트 × 기준점의 조인. 이 레코드의 존재가 '그 프로젝트에서 그 점을 조사했다'는 사실이고,
 * 결과(완전/망실/기타)와 조사 시각을 갖는다. 같은 (프로젝트, 기준점) 쌍은 하나만 존재한다(영속 계층 유니크).
 * 반복조사는 프로젝트가 달라지는 것으로 표현되므로 이 애그리거트에 이력 축을 두지 않는다.
 */
@Getter
public class SurveyRecord {

    private final Long id;
    private final Long projectId;
    private final Long pointId;

    private SurveyResult result;
    private OffsetDateTime surveyedAt;
    private String note;
    private Long surveyedById; // 조사원(회원 id) — 서버가 인증 주체로 채운다(인증 전·파일 임포트는 null)

    private SurveyRecord(
            Long id, Long projectId, Long pointId,
            SurveyResult result, OffsetDateTime surveyedAt, String note, Long surveyedById
    ) {
        this.id = id;
        this.projectId = Objects.requireNonNull(projectId, "프로젝트 ID는 필수입니다.");
        this.pointId = Objects.requireNonNull(pointId, "기준점 ID는 필수입니다.");
        this.result = Objects.requireNonNull(result, "조사 결과는 필수입니다.");
        this.surveyedAt = Objects.requireNonNull(surveyedAt, "조사 시각은 필수입니다.");
        this.note = note;
        this.surveyedById = surveyedById;
    }

    /** 조사 수행 기록 — surveyedAt은 호출자가 Clock(KST)으로 만들어 넘긴다. */
    public static SurveyRecord create(
            Long projectId, Long pointId,
            SurveyResult result, OffsetDateTime surveyedAt, String note, Long surveyedById
    ) {
        return new SurveyRecord(null, projectId, pointId, result, surveyedAt, note, surveyedById);
    }

    /** DB 데이터를 도메인 객체로 복원한다. */
    public static SurveyRecord restore(
            Long id, Long projectId, Long pointId,
            SurveyResult result, OffsetDateTime surveyedAt, String note, Long surveyedById
    ) {
        return new SurveyRecord(id, projectId, pointId, result, surveyedAt, note, surveyedById);
    }

    /** 판정 정정 — 결과·비고를 새 내용으로 교체하고 정정 시각을 조사 시각으로 기록한다. */
    public void revise(SurveyResult result, OffsetDateTime surveyedAt, String note) {
        this.result = Objects.requireNonNull(result, "조사 결과는 필수입니다.");
        this.surveyedAt = Objects.requireNonNull(surveyedAt, "조사 시각은 필수입니다.");
        this.note = note;
    }

    public boolean isLost() {
        return result == SurveyResult.LOST;
    }
}
