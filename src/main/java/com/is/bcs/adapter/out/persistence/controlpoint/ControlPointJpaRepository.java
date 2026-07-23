package com.is.bcs.adapter.out.persistence.controlpoint;

import com.is.bcs.domain.controlpoint.PointType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ControlPointJpaRepository extends JpaRepository<ControlPointJpaEntity, Long> {

    Optional<ControlPointJpaEntity> findByPointNo(String pointNo);

    boolean existsByPointNo(String pointNo);

    @Query("select p.type as type, count(p) as count from ControlPointJpaEntity p group by p.type")
    List<PointTypeCount> countByType();

    interface PointTypeCount {

        PointType getType();

        long getCount();
    }
}
