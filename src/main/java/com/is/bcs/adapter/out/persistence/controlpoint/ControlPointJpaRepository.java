package com.is.bcs.adapter.out.persistence.controlpoint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ControlPointJpaRepository extends JpaRepository<ControlPointJpaEntity, Long> {

    Optional<ControlPointJpaEntity> findByPointNo(String pointNo);

    boolean existsByPointNo(String pointNo);
}
