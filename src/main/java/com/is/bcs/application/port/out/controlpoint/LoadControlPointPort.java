package com.is.bcs.application.port.out.controlpoint;

import com.is.bcs.domain.controlpoint.ControlPoint;

import java.util.List;
import java.util.Optional;

public interface LoadControlPointPort {

    Optional<ControlPoint> findById(Long id);

    Optional<ControlPoint> findByPointNo(String pointNo);

    List<ControlPoint> findAll();

    boolean existsByPointNo(String pointNo);

    long count();
}
