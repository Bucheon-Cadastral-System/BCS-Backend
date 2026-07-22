package com.is.bcs.application.port.in.survey;

public interface CancelSurveyUseCase {

    void cancel(Long projectId, Long pointId);
}
