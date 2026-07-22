package com.is.bcs.application.port.out.survey;

import com.is.bcs.domain.survey.SurveyRecord;

import java.util.List;

public interface SaveSurveyRecordPort {

    SurveyRecord save(SurveyRecord record);

    List<SurveyRecord> saveAll(List<SurveyRecord> records);
}
