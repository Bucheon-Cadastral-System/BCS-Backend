package com.is.bcs.application.port.out.survey;

import java.util.List;

public interface DeleteSurveyTargetPort {

    /** 프로젝트 삭제에 딸려 지운다 — 대상 지정은 프로젝트에 속한 데이터라 홀로 남길 수 없다. */
    void deleteByProjectId(Long projectId);

    /** 대상 재지정에서 빠진 점들만 지운다 — 그 점의 조사 기록도 함께 지워야 한다(기록 포트 몫). */
    void deleteByProjectIdAndPointIds(Long projectId, List<Long> pointIds);
}
