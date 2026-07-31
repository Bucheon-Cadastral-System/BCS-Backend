package com.is.bcs.domain.survey;

import com.is.bcs.domain.survey.exception.InvalidSurveyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SurveyProjectTest {

    private static final LocalDate STARTED = LocalDate.of(2026, 7, 1);

    @Test
    @DisplayName("생성 시 이름은 트림되고 기간·비고가 보존된다")
    void create_keepsAttributes() {
        SurveyProject project = SurveyProject.create(
                " 2026 일제조사 ", STARTED, LocalDate.of(2026, 7, 31), "정기 조사");

        assertEquals("2026 일제조사", project.getName());
        assertEquals(STARTED, project.getStartedOn());
        assertEquals(LocalDate.of(2026, 7, 31), project.getEndedOn());
        assertEquals("정기 조사", project.getNote());
        assertNull(project.getId());
        assertNull(project.getAuthorId()); // 인증이 붙기 전까지 작성자는 비어 있다
    }

    @Test
    @DisplayName("진행 중인 조사는 종료일을 비워 둘 수 있다")
    void create_allowsOpenEndedPeriod() {
        SurveyProject project = SurveyProject.create("진행 중", STARTED, null, null);

        assertNull(project.getEndedOn());
    }

    @Test
    @DisplayName("시작일은 필수다")
    void create_requiresStartDate() {
        assertThrows(InvalidSurveyException.class, () -> SurveyProject.create("이름", null, null, null));
    }

    @Test
    @DisplayName("종료일이 시작일보다 빠르면 거부한다")
    void create_rejectsReversedPeriod() {
        InvalidSurveyException thrown = assertThrows(InvalidSurveyException.class,
                () -> SurveyProject.create("이름", STARTED, STARTED.minusDays(1), null));

        assertEquals("조사 종료일이 시작일보다 빠를 수 없습니다.", thrown.getMessage());
    }

    @Test
    @DisplayName("이름이 비어 있으면 생성·개명할 수 없다")
    void blankName_throws() {
        assertThrows(InvalidSurveyException.class, () -> SurveyProject.create(" ", STARTED, null, null));

        SurveyProject project = SurveyProject.create("정기 조사", STARTED, null, null);
        assertThrows(InvalidSurveyException.class, () -> project.rename(" "));
        assertEquals("정기 조사", project.getName());
    }
}
