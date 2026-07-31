package com.is.bcs.application.port.in.survey;

import com.is.bcs.application.dto.SurveyProgress;
import com.is.bcs.domain.survey.SurveyRecord;

import java.util.List;

public interface GetSurveyRecordsUseCase {

    List<SurveyRecord> getByProjectId(Long projectId);

    SurveyProgress getProgress(Long projectId);

    /** 프로젝트의 조사 대상 점 id. */
    List<Long> getTargetPointIds(Long projectId);
}
