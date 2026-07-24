package com.is.bcs.application.port.out.survey;

import com.is.bcs.domain.survey.SurveyTarget;

import java.util.List;

public interface SaveSurveyTargetPort {

    SurveyTarget save(SurveyTarget target);

    List<SurveyTarget> saveAll(List<SurveyTarget> targets);
}
