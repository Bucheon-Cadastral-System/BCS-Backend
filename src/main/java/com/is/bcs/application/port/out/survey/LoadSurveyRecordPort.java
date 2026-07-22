package com.is.bcs.application.port.out.survey;

import com.is.bcs.domain.survey.SurveyRecord;

import java.util.List;
import java.util.Optional;

public interface LoadSurveyRecordPort {

    List<SurveyRecord> findRecordsByProjectId(Long projectId);

    Optional<SurveyRecord> findRecordByProjectIdAndPointId(Long projectId, Long pointId);
}
