package com.is.bcs.domain.survey.exception;

/** 조사 프로젝트·조사기록 필수값 위반. */
public class InvalidSurveyException extends RuntimeException {

    public InvalidSurveyException(String message) {
        super(message);
    }
}
