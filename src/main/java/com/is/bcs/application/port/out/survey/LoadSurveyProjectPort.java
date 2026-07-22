package com.is.bcs.application.port.out.survey;

import com.is.bcs.domain.survey.SurveyProject;

import java.util.List;
import java.util.Optional;

public interface LoadSurveyProjectPort {

    Optional<SurveyProject> findProjectById(Long id);

    List<SurveyProject> findAllProjects();
}
