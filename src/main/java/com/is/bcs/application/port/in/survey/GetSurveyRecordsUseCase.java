package com.is.bcs.application.port.in.survey;

import com.is.bcs.application.dto.SurveyProgress;
import com.is.bcs.application.dto.SurveyRecordSummary;

import java.util.List;

public interface GetSurveyRecordsUseCase {

    /** 조사원 표시명을 동봉한다 — 점 상세가 '누가 조사했는지'를 그린다. */
    List<SurveyRecordSummary> getByProjectId(Long projectId);

    SurveyProgress getProgress(Long projectId);

    /** 프로젝트의 조사 대상 점 id. */
    List<Long> getTargetPointIds(Long projectId);
}
