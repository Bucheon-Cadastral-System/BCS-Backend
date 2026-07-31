package com.is.bcs.application.port.out.survey;

import java.util.List;

public interface LoadSurveyTargetPort {

    /** 프로젝트의 조사 대상 점 수 — 진행률의 분모(전체 대상)로 쓴다. */
    long countByProjectId(Long projectId);

    /** 프로젝트의 조사 대상 점 id — 지도·목록을 그 조사의 대상으로만 좁힐 때 쓴다. */
    List<Long> findPointIdsByProjectId(Long projectId);
}
