package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.domain.survey.SurveyResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyRecordJpaRepository extends JpaRepository<SurveyRecordJpaEntity, Long> {

    List<SurveyRecordJpaEntity> findByProjectId(Long projectId);

    Optional<SurveyRecordJpaEntity> findByProjectIdAndPointId(Long projectId, Long pointId);

    boolean existsByPointId(Long pointId);

    void deleteByProjectIdAndPointId(Long projectId, Long pointId);

    void deleteByProjectId(Long projectId);

    /** 대상 재지정에서 빠진 점들의 기록. */
    void deleteByProjectIdAndPointIdIn(Long projectId, Collection<Long> pointIds);

    // 진행률은 프로젝트 '대상' 점의 기록만 센다 — 대상 아닌 점의 기록이 조사됨에 섞여 완료가 오탐되지 않도록
    @Query("select r.result as result, count(r) as count from SurveyRecordJpaEntity r"
            + " where r.projectId = :projectId"
            + " and exists (select 1 from SurveyTargetJpaEntity t where t.projectId = r.projectId and t.pointId = r.pointId)"
            + " group by r.result")
    List<ResultCount> countByResult(@Param("projectId") Long projectId);

    interface ResultCount {

        SurveyResult getResult();

        long getCount();
    }

    // 목록의 완료 표시용 일괄 집계 — 진행률과 같은 규칙으로 '대상'인 점의 기록만 센다
    @Query("select r.projectId as projectId, count(r) as cnt from SurveyRecordJpaEntity r"
            + " where exists (select 1 from SurveyTargetJpaEntity t where t.projectId = r.projectId and t.pointId = r.pointId)"
            + " group by r.projectId")
    List<ProjectCount> countSurveyedByProject();

    interface ProjectCount {

        Long getProjectId();

        long getCnt();
    }
}
