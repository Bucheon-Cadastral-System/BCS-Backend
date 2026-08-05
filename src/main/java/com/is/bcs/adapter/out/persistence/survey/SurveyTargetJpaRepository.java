package com.is.bcs.adapter.out.persistence.survey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyTargetJpaRepository extends JpaRepository<SurveyTargetJpaEntity, Long> {

    long countByProjectId(Long projectId);

    boolean existsByPointId(Long pointId);

    /** 점 상세는 화면이 이미 들고 있으므로 id 만 뽑는다. */
    @Query("select t.pointId from SurveyTargetJpaEntity t where t.projectId = :projectId order by t.pointId")
    List<Long> findPointIdsByProjectId(@Param("projectId") Long projectId);

    /** 파생 삭제(엔티티 단위) — 벌크 JPQL 이면 추가 열(@ElementCollection) 행이 남아 FK 에 걸린다. */
    void deleteByProjectId(Long projectId);
}
