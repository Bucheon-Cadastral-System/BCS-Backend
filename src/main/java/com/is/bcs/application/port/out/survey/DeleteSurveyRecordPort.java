package com.is.bcs.application.port.out.survey;

public interface DeleteSurveyRecordPort {

    void deleteByProjectIdAndPointId(Long projectId, Long pointId);
}
