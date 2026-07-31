package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyProjectType;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 도메인 ↔ JPA 엔티티 매핑 왕복 검증. */
class SurveyJpaEntityTest {

    @Test
    @DisplayName("조사 프로젝트 왕복에서 유형·이름·비고가 보존된다")
    void projectRoundTrip_preservesAttributes() {
        SurveyProject origin = SurveyProject.restore(
                3L, SurveyProjectType.GENERAL, "2026 일제조사", "정기 조사");

        SurveyProject restored = SurveyProjectJpaEntity.fromDomain(origin).toDomain();

        assertEquals(3L, restored.getId());
        assertEquals(SurveyProjectType.GENERAL, restored.getType());
        assertEquals("2026 일제조사", restored.getName());
        assertEquals("정기 조사", restored.getNote());
    }

    @Test
    @DisplayName("조사기록 왕복에서 프로젝트×기준점·결과·조사 시각이 보존된다")
    void recordRoundTrip_preservesAttributes() {
        OffsetDateTime surveyedAt = OffsetDateTime.parse("2025-09-08T10:00:00+09:00");
        SurveyRecord origin = SurveyRecord.restore(7L, 1L, 10L, SurveyResult.LOST, surveyedAt, "대상(2건)");

        SurveyRecord restored = SurveyRecordJpaEntity.fromDomain(origin).toDomain();

        assertEquals(7L, restored.getId());
        assertEquals(1L, restored.getProjectId());
        assertEquals(10L, restored.getPointId());
        assertEquals(SurveyResult.LOST, restored.getResult());
        assertEquals(surveyedAt, restored.getSurveyedAt());
        assertEquals("대상(2건)", restored.getNote());
    }

    @Test
    @DisplayName("비고 없는 조사기록은 왕복 후에도 note가 null이다")
    void recordRoundTrip_withoutNote_keepsNull() {
        SurveyRecord origin = SurveyRecord.restore(
                7L, 1L, 10L, SurveyResult.INTACT, OffsetDateTime.parse("2025-09-08T10:00:00+09:00"), null);

        assertNull(SurveyRecordJpaEntity.fromDomain(origin).toDomain().getNote());
    }
}
