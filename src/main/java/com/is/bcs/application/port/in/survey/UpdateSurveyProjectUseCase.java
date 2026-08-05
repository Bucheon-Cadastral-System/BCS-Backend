package com.is.bcs.application.port.in.survey;

import com.is.bcs.application.dto.UpdateSurveyProjectCommand;
import com.is.bcs.domain.survey.SurveyProject;

public interface UpdateSurveyProjectUseCase {

    SurveyProject update(UpdateSurveyProjectCommand command);
}
