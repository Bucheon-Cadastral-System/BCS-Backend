package com.is.bcs.application.port.out.survey;

public interface LoadSurveyTargetPort {

    /** 프로젝트의 조사 대상 점 수 — 진행률의 분모(전체 대상)로 쓴다. */
    long countByProjectId(Long projectId);
}
