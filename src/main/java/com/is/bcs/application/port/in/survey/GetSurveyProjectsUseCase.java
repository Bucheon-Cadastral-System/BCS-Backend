package com.is.bcs.application.port.in.survey;

import com.is.bcs.application.dto.SurveyProjectSummary;
import com.is.bcs.domain.survey.SurveyProject;

import java.util.List;

public interface GetSurveyProjectsUseCase {

    List<SurveyProject> getAll();

    /** 목록 화면용 — 프로젝트마다 대상·조사 수와 작성자 이름을 함께 싣는다(행별 완료 표시·작성자 표기의 근거). */
    List<SurveyProjectSummary> getSummaries();

    SurveyProject getById(Long id);
}
