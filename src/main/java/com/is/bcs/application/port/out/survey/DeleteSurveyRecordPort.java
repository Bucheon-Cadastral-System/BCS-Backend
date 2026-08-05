package com.is.bcs.application.port.out.survey;

import java.util.List;

public interface DeleteSurveyRecordPort {

    void deleteByProjectIdAndPointId(Long projectId, Long pointId);

    /** 프로젝트 삭제에 딸려 지운다 — 조사 기록은 프로젝트에 속한 데이터라 홀로 남길 수 없다. */
    void deleteByProjectId(Long projectId);

    /** 대상 재지정에서 빠진 점들의 기록을 지운다 — 대상 아닌 점의 기록은 어느 화면에도 닿지 않는 주인 없는 행이다. */
    void deleteByProjectIdAndPointIds(Long projectId, List<Long> pointIds);
}
