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

/**
 * 조사기록 저장소.
 *
 * <p>식별자가 (프로젝트, 기준점)이라 프로젝트 하나로 좁히는 조회는 {@code id.projectId} 를 짚는다.
 * 이 열은 기록 행이 직접 들고 있어 대상 테이블로 조인하지 않는다.
 */
@Repository
public interface SurveyRecordJpaRepository extends JpaRepository<SurveyRecordJpaEntity, ProjectPointId> {

    List<SurveyRecordJpaEntity> findByIdProjectId(Long projectId);

    List<SurveyRecordJpaEntity> findByIdPointId(Long pointId);

    /**
     * 그 점의 최신 기록 한 줄. 기본키가 (point_id, project_id) 라 앞자리로 바로 들어간다.
     * 한 점의 기록 수는 그 점을 대상으로 삼은 회차 수뿐이라 정렬 대상도 몇 줄이다.
     *
     * <p>조사 시각이 겹칠 때는 나중에 남긴 기록을 따른다. 파일로 들어온 기록은 조사일의 자정을
     * 시각으로 쓰므로 서로 다른 회차가 같은 날짜를 적으면 시각이 완전히 겹친다.
     * 프로젝트 id 로 가르면 나중에 만든 기록이 옛 회차에 붙었을 때 순서가 뒤집히므로
     * 기록을 만든 시각으로 가른다. 마지막 자리는 그 시각마저 같을 때를 위한 못이다.
     */
    @Query("select r from SurveyRecordJpaEntity r where r.id.pointId = :pointId"
            + " order by r.surveyedAt desc, r.createdAt desc, r.id.projectId desc limit 1")
    Optional<SurveyRecordJpaEntity> findLatestByPointId(@Param("pointId") Long pointId);

    /**
     * 목록에 조사원 이름을 함께 그리는 경로 전용 — 조사원을 조인으로 함께 실어 온다.
     * 이름을 따로 모아 다시 조회하면 문장이 하나 더 나가고, 연관을 그냥 두면 행마다 나간다.
     *
     * <p>정렬을 적어 두지 않으면 DB 가 돌려주는 순서가 그대로 응답 순서가 된다.
     * 그 순서는 갱신·진공 뒤에 바뀌므로 같은 조사를 두 번 열면 줄 차례가 달라진다.
     */
    @Query("select r from SurveyRecordJpaEntity r left join fetch r.surveyor where r.id.projectId = :projectId"
            + " order by r.surveyedAt desc, r.id.pointId asc")
    List<SurveyRecordJpaEntity> findByProjectIdWithSurveyor(@Param("projectId") Long projectId);

    boolean existsByIdPointId(Long pointId);

    /** 없는 기록을 지우라는 요청은 조용히 지나간다 — 취소는 멱등하다. */
    void deleteByIdProjectIdAndIdPointId(Long projectId, Long pointId);

    void deleteByIdProjectId(Long projectId);

    /** 대상 재지정에서 빠진 점들의 기록. */
    void deleteByIdProjectIdAndIdPointIdIn(Long projectId, Collection<Long> pointIds);

    /**
     * 기록의 원자 upsert — 대상 확인·신규 삽입·기존 정정이 전부 한 문장이다.
     * 두 문장(확인 후 쓰기)으로 가르면 동시 기록·대상 재지정이 그 틈에 끼므로 문장 안에 접는다:
     * WHERE EXISTS 가 대상 검사, ON CONFLICT 가 정정(전 필드 교체, 마지막 판정의 주체가 남는다)이다.
     * created_at 은 최초 기록의 것을 지킨다(정정은 갱신이지 재생성이 아니다).
     *
     * <p>외래키도 대상이 아닌 점을 막지만 그것은 최종 거부(예외)다. 이 검사는 "그 점은 이 조사의 대상이
     * 아닙니다"를 404 로 답하기 위한 것이라 문장 안에 그대로 둔다.
     *
     * @return 반영 행 수 — 0 이면 대상이 아니라서 아무것도 쓰지 않은 것.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            insert into bcs.survey_records
                (project_id, point_id, result, surveyed_at, note, surveyed_by, created_at, updated_at)
            select :projectId, :pointId, :result, :surveyedAt, :note, :surveyedBy, :now, :now
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

    // 대상 아닌 점의 기록은 존재할 수 없다(기본키가 곧 대상 참조) — 진행률은 프로젝트로만 좁히면 된다
    @Query("select r.result as result, count(r) as count from SurveyRecordJpaEntity r"
            + " where r.id.projectId = :projectId"
            + " group by r.result")
    List<ResultCount> countByResult(@Param("projectId") Long projectId);

    interface ResultCount {

        SurveyResult getResult();

        long getCount();
    }

    // 목록의 완료 표시용 일괄 집계 — 같은 이유로 대상 여부를 따로 확인하지 않는다
    @Query("select r.id.projectId as projectId, count(r) as cnt from SurveyRecordJpaEntity r"
            + " group by r.id.projectId")
    List<ProjectCount> countSurveyedByProject();

    interface ProjectCount {

        Long getProjectId();

        long getCnt();
    }
}
