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

    /** 이 점에 남은 조사 기록이 있는지(프로젝트 무관) — 기준점 삭제 가부 판정에 쓴다. */
    boolean existsRecordByPointId(Long pointId);
}
