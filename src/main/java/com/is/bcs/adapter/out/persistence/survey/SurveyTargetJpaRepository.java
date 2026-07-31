package com.is.bcs.adapter.out.persistence.survey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyTargetJpaRepository extends JpaRepository<SurveyTargetJpaEntity, Long> {

    long countByProjectId(Long projectId);

    /** 점 상세는 화면이 이미 들고 있으므로 id 만 뽑는다. */
    @Query("select t.pointId from SurveyTargetJpaEntity t where t.projectId = :projectId order by t.pointId")
    List<Long> findPointIdsByProjectId(@Param("projectId") Long projectId);
}
