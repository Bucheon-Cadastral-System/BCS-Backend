package com.is.bcs.adapter.out.persistence.survey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyRecordJpaRepository extends JpaRepository<SurveyRecordJpaEntity, Long> {

    List<SurveyRecordJpaEntity> findByProjectId(Long projectId);

    Optional<SurveyRecordJpaEntity> findByProjectIdAndPointId(Long projectId, Long pointId);
}
