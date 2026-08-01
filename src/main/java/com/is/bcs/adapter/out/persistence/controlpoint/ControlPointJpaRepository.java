package com.is.bcs.adapter.out.persistence.controlpoint;

import com.is.bcs.domain.controlpoint.PointType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
}
