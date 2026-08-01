package com.is.bcs.domain.survey;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 조사 대상 — 조사 프로젝트가 조사 책임지는 기준점. (프로젝트, 기준점) 쌍은 하나만 존재한다(영속 계층 유니크).
 * 조사기록(SurveyRecord)과 분리한다: 대상이지만 아직 조사하지 않은 점(미조사)을 표현하려면 조사됨 축과 별개의 대상 집합이 필요하다.
 * 진행률의 분모(전체 대상 수)는 이 대상 집합으로 세고, 분자(조사됨)는 조사기록으로 센다.
 *
 * 대상지 파일의 한 행이 곧 하나의 대상이므로, 기본 양식에 없어 기준점 마스터로는 못 옮긴 열도 여기에 보관한다.
 * 같은 기준점이라도 조사마다 파일이 달라 값이 다를 수 있어 기준점이 아니라 대상에 둔다.
 */
@Getter
public class SurveyTarget {

    private final Long id;
    private final Long projectId;
    private final Long pointId;
    private final List<ExtraColumn> extras;

    private SurveyTarget(Long id, Long projectId, Long pointId, List<ExtraColumn> extras) {
        this.id = id;
        this.projectId = Objects.requireNonNull(projectId, "프로젝트 ID는 필수입니다.");
        this.pointId = Objects.requireNonNull(pointId, "기준점 ID는 필수입니다.");
        // 값이 비어 있는 열도 그대로 담기므로 null 원소를 허용하는 복사를 쓴다
        this.extras = extras == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(extras));
    }

    public static SurveyTarget create(Long projectId, Long pointId) {
        return create(projectId, pointId, List.of());
    }

    public static SurveyTarget create(Long projectId, Long pointId, List<ExtraColumn> extras) {
        return new SurveyTarget(null, projectId, pointId, extras);
    }

    /** DB 데이터를 도메인 객체로 복원한다. */
    public static SurveyTarget restore(Long id, Long projectId, Long pointId, List<ExtraColumn> extras) {
        return new SurveyTarget(id, projectId, pointId, extras);
    }
}
