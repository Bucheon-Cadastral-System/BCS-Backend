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
        SurveyProject project = SurveyProject.create(null, 
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
        SurveyProject project = SurveyProject.create(null, "진행 중", STARTED, null, null);

        assertNull(project.getEndedOn());
    }

    @Test
    @DisplayName("시작일은 필수다")
    void create_requiresStartDate() {
        assertThrows(InvalidSurveyException.class, () -> SurveyProject.create(null, "이름", null, null, null));
    }

    @Test
    @DisplayName("종료일이 시작일보다 빠르면 거부한다")
    void create_rejectsReversedPeriod() {
        InvalidSurveyException thrown = assertThrows(InvalidSurveyException.class,
                () -> SurveyProject.create(null, "이름", STARTED, STARTED.minusDays(1), null));

        assertEquals("조사 종료일이 시작일보다 빠를 수 없습니다.", thrown.getMessage());
    }

    @Test
    @DisplayName("시작일과 종료일이 같은 하루짜리 조사는 허용한다")
    void create_allowsSameDayPeriod() {
        SurveyProject project = SurveyProject.create(null, "당일 조사", STARTED, STARTED, null);

        assertEquals(STARTED, project.getStartedOn());
        assertEquals(STARTED, project.getEndedOn());
    }

    @Test
    @DisplayName("이름이 비어 있으면 생성·개명할 수 없다")
    void blankName_throws() {
        assertThrows(InvalidSurveyException.class, () -> SurveyProject.create(null, " ", STARTED, null, null));

        SurveyProject project = SurveyProject.create(null, "정기 조사", STARTED, null, null);
        assertThrows(InvalidSurveyException.class, () -> project.rename(" "));
        assertEquals("정기 조사", project.getName());
    }

    @Test
    @DisplayName("수정은 생성과 같은 검증을 거쳐 이름·기간·비고를 통째로 바꾼다")
    void update_replacesAllValues() {
        SurveyProject project = SurveyProject.create(null, "이름", STARTED, null, "비고");

        project.update(" 새 이름 ", STARTED.plusDays(1), STARTED.plusDays(10), null);

        assertEquals("새 이름", project.getName());
        assertEquals(STARTED.plusDays(1), project.getStartedOn());
        assertEquals(STARTED.plusDays(10), project.getEndedOn());
        assertNull(project.getNote()); // 비고는 비울 수 있는 값이라 null 이 곧 지움이다
    }

    @Test
    @DisplayName("수정이 거부되면 기존 값이 그대로 남는다 — 일부만 바뀐 채로 남지 않는다")
    void update_rejected_keepsOriginalValues() {
        SurveyProject project = SurveyProject.create(null, "이름", STARTED, null, "비고");

        assertThrows(InvalidSurveyException.class, () -> project.update(" ", STARTED, null, null));
        assertThrows(InvalidSurveyException.class,
                () -> project.update("새 이름", STARTED, STARTED.minusDays(1), null));

        assertEquals("이름", project.getName());
        assertEquals(STARTED, project.getStartedOn());
        assertNull(project.getEndedOn());
        assertEquals("비고", project.getNote());
    }
}
