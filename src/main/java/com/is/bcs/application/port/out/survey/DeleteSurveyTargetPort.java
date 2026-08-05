package com.is.bcs.application.port.out.survey;

public interface DeleteSurveyTargetPort {

    /** 프로젝트 삭제에 딸려 지운다 — 대상 지정은 프로젝트에 속한 데이터라 홀로 남길 수 없다. */
    void deleteByProjectId(Long projectId);
}
