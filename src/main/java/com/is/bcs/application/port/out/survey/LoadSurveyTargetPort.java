package com.is.bcs.application.port.out.survey;

import java.util.List;

public interface LoadSurveyTargetPort {

    /** 프로젝트의 조사 대상 점 수 — 진행률의 분모(전체 대상)로 쓴다. */
    long countByProjectId(Long projectId);

    /** 프로젝트의 조사 대상 점 id — 지도·목록을 그 조사의 대상으로만 좁힐 때 쓴다. */
    List<Long> findPointIdsByProjectId(Long projectId);

    /** 이 점을 대상으로 지정한 조사가 있는지 — 기준점 삭제 가부 판정에 쓴다. */
    boolean existsByPointId(Long pointId);

    /** 이 점이 그 프로젝트의 대상인지 — 조사 기록은 대상으로 지정한 점에만 남길 수 있다. */
    boolean existsByProjectIdAndPointId(Long projectId, Long pointId);
}
