package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.domain.survey.SurveyResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyRecordJpaRepository extends JpaRepository<SurveyRecordJpaEntity, Long> {

    List<SurveyRecordJpaEntity> findByProjectId(Long projectId);

    Optional<SurveyRecordJpaEntity> findByProjectIdAndPointId(Long projectId, Long pointId);

    void deleteByProjectIdAndPointId(Long projectId, Long pointId);

    @Query("select r.result as result, count(r) as count from SurveyRecordJpaEntity r"
            + " where r.projectId = :projectId group by r.result")
    List<ResultCount> countByResult(@Param("projectId") Long projectId);

    interface ResultCount {

        SurveyResult getResult();

        long getCount();
    }
}
