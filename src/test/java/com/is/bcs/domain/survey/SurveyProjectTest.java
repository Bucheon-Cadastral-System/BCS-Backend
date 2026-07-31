package com.is.bcs.domain.survey;

import com.is.bcs.domain.survey.exception.InvalidSurveyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SurveyProjectTest {

    @Test
    @DisplayName("생성 시 이름은 트림되고 유형·비고가 보존된다")
    void create_keepsAttributes() {
        SurveyProject project = SurveyProject.create(
                SurveyProjectType.GENERAL, " 2026 일제조사 ", "정기 조사");

        assertEquals("2026 일제조사", project.getName());
        assertEquals(SurveyProjectType.GENERAL, project.getType());
        assertEquals("정기 조사", project.getNote());
        assertNull(project.getId());
    }

    @Test
    @DisplayName("이름이 비어 있으면 생성·개명할 수 없다")
    void blankName_throws() {
        assertThrows(InvalidSurveyException.class,
                () -> SurveyProject.create(SurveyProjectType.GENERAL, " ", null));

        SurveyProject project = SurveyProject.create(SurveyProjectType.GENERAL, "정기 조사", null);
        assertThrows(InvalidSurveyException.class, () -> project.rename(" "));
        assertEquals("정기 조사", project.getName());
    }
}
