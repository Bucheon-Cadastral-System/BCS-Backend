package com.is.bcs.adapter.out.persistence.survey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyTargetJpaRepository extends JpaRepository<SurveyTargetJpaEntity, Long> {

    long countByProjectId(Long projectId);
}
