package com.is.bcs.application.port.out.survey;

import com.is.bcs.domain.survey.SurveyProject;

public interface SaveSurveyProjectPort {

    SurveyProject save(SurveyProject project);
}
