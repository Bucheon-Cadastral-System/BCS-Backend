package com.is.bcs.application.port.in.survey;

import com.is.bcs.application.dto.CreateSurveyProjectCommand;
import com.is.bcs.domain.survey.SurveyProject;

public interface CreateSurveyProjectUseCase {

    SurveyProject create(CreateSurveyProjectCommand command);
}
