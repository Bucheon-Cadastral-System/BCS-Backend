package com.is.bcs.application.port.in.survey;

import com.is.bcs.application.dto.RecordSurveyCommand;
import com.is.bcs.domain.survey.SurveyRecord;

public interface RecordSurveyUseCase {

    SurveyRecord record(RecordSurveyCommand command);
}
