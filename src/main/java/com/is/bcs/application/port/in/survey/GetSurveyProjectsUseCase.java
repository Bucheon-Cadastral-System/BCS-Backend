package com.is.bcs.application.port.in.survey;

import com.is.bcs.domain.survey.SurveyProject;

import java.util.List;

public interface GetSurveyProjectsUseCase {

    List<SurveyProject> getAll();

    SurveyProject getById(Long id);
}
