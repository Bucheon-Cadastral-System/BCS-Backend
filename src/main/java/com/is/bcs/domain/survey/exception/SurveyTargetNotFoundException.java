package com.is.bcs.domain.survey.exception;

/** 프로젝트의 조사 대상이 아닌 기준점 — 조사 기록은 대상으로 지정한 점에만 남길 수 있다. */
public class SurveyTargetNotFoundException extends RuntimeException {

    public SurveyTargetNotFoundException(String message) {
        super(message);
    }
}
