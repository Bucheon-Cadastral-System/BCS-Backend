package com.is.bcs.domain.survey;

import com.is.bcs.domain.survey.exception.InvalidSurveyException;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 조사 프로젝트 — 기준점 조사를 묶는 단위(예: 정기 조사 1회, 협의 1건).
 * 점별 조사 여부·결과는 이 애그리거트가 아니라 조사기록(SurveyRecord)이 갖는다.
 *
 * 조사를 하게 된 계기는 따로 담지 않는다 — 고객사는 조사마다 그때그때 이름을 붙이므로
 * 되풀이되는 분류로 쓸 수 없고, 조사명이 그 역할을 한다.
 */
@Getter
public class SurveyProject {

    private final Long id;
    /** 작성자(회원 id) — 인증이 붙기 전까지는 비어 있다. 클라이언트가 보낸 값을 믿지 않고 서버가 채워야 한다. */
    private final Long authorId;

    private String name;
    private LocalDate startedOn;
    private LocalDate endedOn; // 진행 중이면 비어 있다
    private String note; // 협의 문서번호 등 자유 비고

    private SurveyProject(Long id, Long authorId, String name, LocalDate startedOn, LocalDate endedOn, String note) {
        this.id = id;
        this.authorId = authorId;
        this.name = requireText(name, "조사명");
        this.startedOn = requirePeriod(startedOn, endedOn);
        this.endedOn = endedOn;
        this.note = note;
    }

    public static SurveyProject create(String name, LocalDate startedOn, LocalDate endedOn, String note) {
        return new SurveyProject(null, null, name, startedOn, endedOn, note);
    }

    /** DB 데이터를 도메인 객체로 복원한다. */
    public static SurveyProject restore(
            Long id, Long authorId, String name, LocalDate startedOn, LocalDate endedOn, String note) {
        return new SurveyProject(id, authorId, name, startedOn, endedOn, note);
    }

    public void rename(String name) {
        this.name = requireText(name, "조사명");
    }

    /** 값 전체 교체 — 생성과 같은 검증을 거친다. 종료일·비고는 비울 수 있는 값이라 null 이 곧 지움이다. */
    public void update(String name, LocalDate startedOn, LocalDate endedOn, String note) {
        // 검증을 모두 통과한 뒤에 대입한다 — 거부된 수정이 일부 값만 바꿔 놓으면 안 된다
        String newName = requireText(name, "조사명");
        LocalDate newStartedOn = requirePeriod(startedOn, endedOn);
        this.name = newName;
        this.startedOn = newStartedOn;
        this.endedOn = endedOn;
        this.note = note;
    }

    private static LocalDate requirePeriod(LocalDate startedOn, LocalDate endedOn) {
        if (startedOn == null) {
            throw new InvalidSurveyException("조사 시작일은 필수입니다.");
        }
        if (endedOn != null && endedOn.isBefore(startedOn)) {
            throw new InvalidSurveyException("조사 종료일이 시작일보다 빠를 수 없습니다.");
        }
        return startedOn;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidSurveyException(fieldName + "은(는) 필수입니다.");
        }
        return value.trim();
    }
}
