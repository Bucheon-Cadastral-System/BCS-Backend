package com.is.bcs.domain.survey.exception;

/** 조사 프로젝트 조회 실패. */
public class SurveyProjectNotFoundException extends RuntimeException {

    public SurveyProjectNotFoundException(String message) {
        super(message);
    }
}
