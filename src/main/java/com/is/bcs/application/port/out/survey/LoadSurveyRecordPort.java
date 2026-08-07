package com.is.bcs.application.port.out.survey;

import com.is.bcs.application.dto.SurveyRecordSummary;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadSurveyRecordPort {

    List<SurveyRecord> findRecordsByProjectId(Long projectId);

    /** 이 기준점의 모든 회차 기록 — 마스터 최종조사 요약을 다시 맞출 때 쓴다. */
    List<SurveyRecord> findRecordsByPointId(Long pointId);

    /** 기록과 조사원 표시명을 한 문장으로 가져온다 — 목록 화면 전용. */
    List<SurveyRecordSummary> findRecordSummariesByProjectId(Long projectId);

    Optional<SurveyRecord> findRecordByProjectIdAndPointId(Long projectId, Long pointId);

    /** 프로젝트의 결과별 조사기록 개수 — 기록된 결과만 키로 담는다(집계는 DB가 한다). */
    Map<SurveyResult, Long> countByResult(Long projectId);

    /** 전 프로젝트의 조사된 점 수(대상인 점만) — 목록이 행마다 완료 여부를 그릴 때 한 번에 싣는다. */
    Map<Long, Long> countSurveyedByProject();

    /** 이 점에 남은 조사 기록이 있는지(프로젝트 무관) — 기준점 삭제 가부 판정에 쓴다. */
    boolean existsRecordByPointId(Long pointId);
}
