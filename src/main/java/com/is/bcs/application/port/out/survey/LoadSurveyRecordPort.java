package com.is.bcs.application.port.out.survey;

import com.is.bcs.application.dto.PointLastSurvey;
import com.is.bcs.application.dto.SurveyRecordSummary;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadSurveyRecordPort {

    List<SurveyRecord> findRecordsByProjectId(Long projectId);

    /** 이 기준점의 모든 회차 기록 — 마스터 최종조사 요약을 다시 맞출 때 쓴다. */
    List<SurveyRecord> findRecordsByPointId(Long pointId);

    /**
     * 이 점의 기록 중 가장 최근 것 하나.
     *
     * <p>조사 시각이 늦은 것을 따르고, 같으면 나중에 만든 조사를 따른다. 파일로 들어온 기록은 조사일의 자정을
     * 시각으로 쓰므로 서로 다른 회차가 같은 점을 같은 날짜로 적으면 시각이 완전히 겹친다.
     * 기준을 두지 않으면 조회 순서가 바뀔 때마다 답이 달라진다.
     */
    Optional<SurveyRecord> findLatestRecordByPointId(Long pointId);

    /**
     * 점마다 가장 최근 기록 하나씩, 기록이 있는 점만.
     *
     * <p>{@link #findLatestRecordByPointId} 를 점 수만큼 부르는 것과 답이 같아야 하므로 순서 기준도 같다.
     * 조사 시각이 늦은 것을 따르고, 겹치면 나중에 만든 기록을, 그것마저 겹치면 프로젝트 id 가 큰 쪽을 따른다.
     *
     * <p>조사원과 비고는 싣지 않는다. 지도가 색으로 옮기는 데는 결과와 조사일이면 되고,
     * 조사원 이름까지 실으면 점 수만큼 회원 조인이 붙는다.
     */
    List<PointLastSurvey> findLatestSurveyPerPoint();

    /** 기록과 조사원 표시명을 한 문장으로 가져온다 — 목록 화면 전용. */
    List<SurveyRecordSummary> findRecordSummariesByProjectId(Long projectId);

    /**
     * 이 점들의 최신 기록 한 줄씩, 조사원 표시명까지 — 점 목록이 정해진 자리(내보내기) 전용.
     *
     * <p>{@link #findLatestSurveyPerPoint} 와 고르는 줄은 같고 조사원·비고를 더 싣는다. 점 전체가 아니라
     * 물어본 점만 훑으므로 회원 조인이 붙어도 문장 하나로 끝난다. 기록이 없는 점은 담기지 않는다.
     */
    List<SurveyRecordSummary> findLatestRecordSummariesByPointIds(Collection<Long> pointIds);

    Optional<SurveyRecord> findRecordByProjectIdAndPointId(Long projectId, Long pointId);

    /** 프로젝트의 결과별 조사기록 개수 — 기록된 결과만 키로 담는다(집계는 DB가 한다). */
    Map<SurveyResult, Long> countByResult(Long projectId);

    /** 전 프로젝트의 조사된 점 수(대상인 점만) — 목록이 행마다 완료 여부를 그릴 때 한 번에 싣는다. */
    Map<Long, Long> countSurveyedByProject();

    /** 이 점에 남은 조사 기록이 있는지(프로젝트 무관) — 기준점 삭제 가부 판정에 쓴다. */
    boolean existsRecordByPointId(Long pointId);
}
