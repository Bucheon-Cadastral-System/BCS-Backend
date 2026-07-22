package com.is.bcs.domain.survey;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 조사 프로젝트 유형 — 굴착협의처럼 계기가 있는 조사와 일반(정기) 조사를 구분한다. */
@Getter
@RequiredArgsConstructor
public enum SurveyProjectType {

    GENERAL("일반 조사"),
    EXCAVATION_CONSULTATION("굴착협의");

    private final String displayName;
}
