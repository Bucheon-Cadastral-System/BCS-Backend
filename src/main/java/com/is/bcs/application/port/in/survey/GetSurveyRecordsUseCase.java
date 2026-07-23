package com.is.bcs.application.port.in.survey;

import com.is.bcs.application.dto.SurveyProgress;
import com.is.bcs.domain.survey.SurveyRecord;

import java.util.List;

public interface GetSurveyRecordsUseCase {

    List<SurveyRecord> getByProjectId(Long projectId);

    SurveyProgress getProgress(Long projectId);
}
