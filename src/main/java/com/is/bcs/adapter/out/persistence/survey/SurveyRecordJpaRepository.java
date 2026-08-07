package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.domain.survey.SurveyResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyRecordJpaRepository extends JpaRepository<SurveyRecordJpaEntity, Long> {

    List<SurveyRecordJpaEntity> findByProjectId(Long projectId);

    List<SurveyRecordJpaEntity> findByPointId(Long pointId);

    /**
     * 목록에 조사원 이름을 함께 그리는 경로 전용 — 조사원을 조인으로 함께 실어 온다.
     * 이름을 따로 모아 다시 조회하면 문장이 하나 더 나가고, 연관을 그냥 두면 행마다 나간다.
     */
    @Query("select r from SurveyRecordJpaEntity r left join fetch r.surveyor where r.project.id = :projectId")
    List<SurveyRecordJpaEntity> findByProjectIdWithSurveyor(@Param("projectId") Long projectId);

    Optional<SurveyRecordJpaEntity> findByProjectIdAndPointId(Long projectId, Long pointId);

    boolean existsByPointId(Long pointId);

    void deleteByProjectIdAndPointId(Long projectId, Long pointId);

    void deleteByProjectId(Long projectId);

    /** 대상 재지정에서 빠진 점들의 기록. */
    void deleteByProjectIdAndPointIdIn(Long projectId, Collection<Long> pointIds);

    /**
     * 기록의 원자 upsert — 대상 확인·신규 삽입·기존 정정이 전부 한 문장이다.
     * 두 문장(확인 후 쓰기)으로 가르면 동시 기록·대상 재지정이 그 틈에 끼므로 문장 안에 접는다:
     * WHERE EXISTS 가 대상 검사, ON CONFLICT 가 정정(전 필드 교체, 마지막 판정의 주체가 남는다)이다.
     * created_at 은 최초 기록의 것을 지킨다(정정은 갱신이지 재생성이 아니다).
     *
     * @return 반영 행 수 — 0 이면 대상이 아니라서 아무것도 쓰지 않은 것.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            insert into bcs.survey_records
                (id, project_id, point_id, result, surveyed_at, note, surveyed_by, created_at, updated_at)
            select nextval('bcs.survey_records_seq'), :projectId, :pointId, :result, :surveyedAt, :note, :surveyedBy, :now, :now
            where exists (
                select 1 from bcs.survey_targets t
                where t.project_id = :projectId and t.point_id = :pointId)
            on conflict (project_id, point_id) do update set
                result = excluded.result,
                surveyed_at = excluded.surveyed_at,
                note = excluded.note,
                surveyed_by = excluded.surveyed_by,
                updated_at = excluded.updated_at
            """, nativeQuery = true)
    int upsertForTarget(
            @Param("projectId") Long projectId,
            @Param("pointId") Long pointId,
            @Param("result") String result,
            @Param("surveyedAt") OffsetDateTime surveyedAt,
            @Param("note") String note,
            @Param("surveyedBy") Long surveyedBy,
            @Param("now") OffsetDateTime now);

    // 진행률은 프로젝트 '대상' 점의 기록만 센다 — 대상 아닌 점의 기록이 조사됨에 섞여 완료가 오탐되지 않도록
    @Query("select r.result as result, count(r) as count from SurveyRecordJpaEntity r"
            + " where r.project.id = :projectId"
            + " and exists (select 1 from SurveyTargetJpaEntity t where t.project.id = r.project.id and t.point.id = r.point.id)"
            + " group by r.result")
    List<ResultCount> countByResult(@Param("projectId") Long projectId);

    interface ResultCount {

        SurveyResult getResult();

        long getCount();
    }

    // 목록의 완료 표시용 일괄 집계 — 진행률과 같은 규칙으로 '대상'인 점의 기록만 센다
    @Query("select r.project.id as projectId, count(r) as cnt from SurveyRecordJpaEntity r"
            + " where exists (select 1 from SurveyTargetJpaEntity t where t.project.id = r.project.id and t.point.id = r.point.id)"
            + " group by r.project.id")
    List<ProjectCount> countSurveyedByProject();

    interface ProjectCount {

        Long getProjectId();

        long getCnt();
    }
}
