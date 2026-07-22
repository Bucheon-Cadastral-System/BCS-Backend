package com.is.bcs.domain.survey.exception;

/** 조사기록 조회 실패. */
public class SurveyRecordNotFoundException extends RuntimeException {

    public SurveyRecordNotFoundException(String message) {
        super(message);
    }
}
