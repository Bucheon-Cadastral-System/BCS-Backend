package com.is.bcs.adapter.out.persistence.controlpointimage;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ControlPointImageJpaRepository extends JpaRepository<ControlPointImageJpaEntity, Long> {

    Optional<ControlPointImageJpaEntity> findByProjectIdAndPointId(Long projectId, Long pointId);

    boolean existsByProjectIdAndPointId(Long projectId, Long pointId);

    Page<ControlPointImageJpaEntity> findAllByPointId(Long pointId, Pageable pageable);

    Page<ControlPointImageJpaEntity> findAllByProjectId(Long projectId, Pageable pageable);


}