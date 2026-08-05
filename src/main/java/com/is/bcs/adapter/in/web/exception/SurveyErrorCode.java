package com.is.bcs.adapter.in.web.exception;

import org.springframework.http.HttpStatus;

/** 조사(SURVEY_) 도메인 에러 코드. */
public enum SurveyErrorCode implements ErrorCode {

    /** 조사 프로젝트 없음 */
    SURVEY_PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND),

    /** 조사기록 없음 */
    SURVEY_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND),

    /** 프로젝트의 조사 대상이 아닌 기준점 */
    SURVEY_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND),

    /** 조사 프로젝트·조사기록 필수값 위반 */
    SURVEY_INVALID(HttpStatus.BAD_REQUEST);

    private final HttpStatus status;

    SurveyErrorCode(HttpStatus status) {
        this.status = status;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus status() {
        return status;
    }
}
