package com.is.bcs.application.port.in.survey;

import com.is.bcs.application.dto.RecordSurveyCommand;
import com.is.bcs.application.dto.SurveyRecordSummary;

public interface RecordSurveyUseCase {

    /** 조사원 표시명을 동봉해 돌려준다 — 화면이 응답만으로 '누가 조사했는지'를 그릴 수 있게. */
    SurveyRecordSummary record(RecordSurveyCommand command);
}
