package com.is.bcs.domain.survey;

import com.is.bcs.domain.survey.exception.InvalidSurveyException;
import lombok.Getter;

import java.util.Objects;

/**
 * 조사 프로젝트 — 기준점 조사를 묶는 단위(예: 굴착협의 1건, 정기 조사 1회).
 * 점별 조사 여부·결과는 이 애그리거트가 아니라 조사기록(SurveyRecord)이 갖는다.
 */
@Getter
public class SurveyProject {

    private final Long id;
    private final SurveyProjectType type;

    private String name;
    private String note; // 협의 문서번호 등 자유 비고

    private SurveyProject(Long id, SurveyProjectType type, String name, String note) {
        this.id = id;
        this.type = Objects.requireNonNull(type, "유형은 필수입니다.");
        this.name = requireText(name, "조사명");
        this.note = note;
    }

    public static SurveyProject create(SurveyProjectType type, String name, String note) {
        return new SurveyProject(null, type, name, note);
    }

    /** DB 데이터를 도메인 객체로 복원한다. */
    public static SurveyProject restore(Long id, SurveyProjectType type, String name, String note) {
        return new SurveyProject(id, type, name, note);
    }

    public void rename(String name) {
        this.name = requireText(name, "조사명");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidSurveyException(fieldName + "은(는) 필수입니다.");
        }
        return value.trim();
    }
}
