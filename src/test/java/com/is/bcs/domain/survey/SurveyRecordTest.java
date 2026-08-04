package com.is.bcs.domain.survey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurveyRecordTest {

    private static final OffsetDateTime SURVEYED_AT = OffsetDateTime.parse("2025-09-08T10:00:00+09:00");

    @Test
    @DisplayName("조사기록 생성 — 프로젝트×기준점·결과·조사 시각을 갖는다")
    void create_keepsAttributes() {
        SurveyRecord record = SurveyRecord.create(1L, 10L, SurveyResult.INTACT, SURVEYED_AT, "대상(2건)", 5L);

        assertEquals(1L, record.getProjectId());
        assertEquals(10L, record.getPointId());
        assertEquals(SurveyResult.INTACT, record.getResult());
        assertEquals(SURVEYED_AT, record.getSurveyedAt());
        assertEquals("대상(2건)", record.getNote());
        assertEquals(5L, record.getSurveyedById());
        assertFalse(record.isLost());
    }

    @Test
    @DisplayName("판정 정정 — 결과·비고·조사 시각이 새 내용으로 교체된다")
    void revise_replacesResultNoteAndTime() {
        SurveyRecord record = SurveyRecord.create(1L, 10L, SurveyResult.INTACT, SURVEYED_AT, "최초 비고", null);
        OffsetDateTime revisedAt = SURVEYED_AT.plusDays(1);

        record.revise(SurveyResult.LOST, revisedAt, "정정 비고");

        assertTrue(record.isLost());
        assertEquals(revisedAt, record.getSurveyedAt());
        assertEquals("정정 비고", record.getNote());

        record.revise(SurveyResult.INTACT, revisedAt, null);
        assertEquals(null, record.getNote()); // 비고 없는 정정은 비고를 지운다(전체 교체)
    }

    @Test
    @DisplayName("필수값(프로젝트·기준점·결과·시각) 없이 만들 수 없다")
    void create_requiresMandatoryFields() {
        assertThrows(NullPointerException.class,
                () -> SurveyRecord.create(null, 10L, SurveyResult.INTACT, SURVEYED_AT, null, null));
        assertThrows(NullPointerException.class,
                () -> SurveyRecord.create(1L, null, SurveyResult.INTACT, SURVEYED_AT, null, null));
        assertThrows(NullPointerException.class,
                () -> SurveyRecord.create(1L, 10L, null, SURVEYED_AT, null, null));
        assertThrows(NullPointerException.class,
                () -> SurveyRecord.create(1L, 10L, SurveyResult.INTACT, null, null, null));
    }
}
