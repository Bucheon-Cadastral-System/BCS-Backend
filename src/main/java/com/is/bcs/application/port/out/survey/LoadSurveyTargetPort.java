package com.is.bcs.application.port.out.survey;

import java.util.List;

public interface LoadSurveyTargetPort {

    /** 프로젝트의 조사 대상 점 수 — 진행률의 분모(전체 대상)로 쓴다. */
    long countByProjectId(Long projectId);

    /** 전 프로젝트의 대상 점 수 — 목록이 행마다 완료 여부를 그릴 때 한 번에 싣는다. */
    java.util.Map<Long, Long> countTargetsByProject();

    /** 프로젝트의 조사 대상 점 id — 지도·목록을 그 조사의 대상으로만 좁힐 때 쓴다. */
    List<Long> findPointIdsByProjectId(Long projectId);

    /** 이 점을 대상으로 지정한 조사가 있는지 — 기준점 삭제 가부 판정에 쓴다. */
    boolean existsByPointId(Long pointId);

    boolean lockByProjectIdAndPointId(Long projectId, Long pointId);
}
