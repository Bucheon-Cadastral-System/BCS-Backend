package com.is.bcs.application.port.out.survey;

import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadSurveyRecordPort {

    List<SurveyRecord> findRecordsByProjectId(Long projectId);

    Optional<SurveyRecord> findRecordByProjectIdAndPointId(Long projectId, Long pointId);

    /** 프로젝트의 결과별 조사기록 개수 — 기록된 결과만 키로 담는다(집계는 DB가 한다). */
    Map<SurveyResult, Long> countByResult(Long projectId);
}
