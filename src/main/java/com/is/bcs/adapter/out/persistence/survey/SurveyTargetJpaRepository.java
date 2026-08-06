package com.is.bcs.adapter.out.persistence.survey;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyTargetJpaRepository extends JpaRepository<SurveyTargetJpaEntity, Long> {

    long countByProjectId(Long projectId);

    boolean existsByPointId(Long pointId);

    /** 점 상세는 화면이 이미 들고 있으므로 id 만 뽑는다. */
    @Query("select t.pointId from SurveyTargetJpaEntity t where t.projectId = :projectId order by t.pointId")
    List<Long> findPointIdsByProjectId(@Param("projectId") Long projectId);

    /** 파생 삭제(엔티티 단위) — 벌크 JPQL 이면 추가 열(@ElementCollection) 행이 남아 FK 에 걸린다. */
    void deleteByProjectId(Long projectId);

    /** 프로젝트별 대상 점 수 — 목록의 완료 표시용 일괄 집계. */
    @Query("select t.projectId as projectId, count(t) as cnt from SurveyTargetJpaEntity t group by t.projectId")
    List<ProjectCount> countByProject();

    interface ProjectCount {

        Long getProjectId();

        long getCnt();
    }

    /** 대상 재지정에서 빠진 점들 — 같은 이유로 파생 삭제(엔티티 단위)를 쓴다. */
    void deleteByProjectIdAndPointIdIn(Long projectId, Collection<Long> pointIds);

    /** 파일 업로드 시 비관적 락 활용하여 조회  */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select target
        from SurveyTargetJpaEntity target
        where target.projectId = :projectId
          and target.pointId = :pointId
        """)
    Optional<SurveyTargetJpaEntity> findByProjectIdAndPointIdForUpdate(@Param("projectId") Long projectId, @Param("pointId") Long pointId);

}
