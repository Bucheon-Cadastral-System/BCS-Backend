package com.is.bcs.adapter.out.persistence.controlpoint;

import com.is.bcs.domain.controlpoint.PointType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ControlPointJpaRepository extends JpaRepository<ControlPointJpaEntity, Long> {

    Optional<ControlPointJpaEntity> findByPointNo(String pointNo);

    Optional<ControlPointJpaEntity> findFirstByNameAndType(String name, PointType type);

    List<ControlPointJpaEntity> findAllByNameIn(Collection<String> names);

    List<ControlPointJpaEntity> findAllByPointNoIn(Collection<String> pointNos);

    boolean existsByPointNo(String pointNo);

    @Query("select p.type as type, count(p) as count from ControlPointJpaEntity p group by p.type")
    List<PointTypeCount> countByType();

    interface PointTypeCount {

        PointType getType();

        long getCount();
    }

    /**
     * 시드 최종조사가 적힌 점만 — 판정이 빈 행은 지도가 고를 색이 없으므로 거른다.
     * 점을 통째로 읽어 걸러 내면 쓰지 않을 스무 남짓 열을 수천 행만큼 나른다.
     *
     * <p>공백만 든 값도 빈 값으로 본다. 그대로 통과시키면 어휘로 되돌릴 말이 없어 기타로 담기고,
     * 조사한 적 없는 점이 조사된 점으로 응답에 실린다. 같은 포트의 다른 구현도 같은 기준을 쓴다.
     */
    @Query("select p.id as pointId, p.lastSurveyResult as result, p.lastSurveyedOn as surveyedOn"
            + " from ControlPointJpaEntity p where p.lastSurveyResult is not null and trim(p.lastSurveyResult) <> ''")
    List<SeedLastSurvey> findSeedLastSurveys();

    interface SeedLastSurvey {

        Long getPointId();

        /** 최종조사내용 — 임포트가 표시명으로 맞춰 두지만 모르는 문구는 원문 그대로 남아 있다. */
        String getResult();

        LocalDate getSurveyedOn();
    }
}
