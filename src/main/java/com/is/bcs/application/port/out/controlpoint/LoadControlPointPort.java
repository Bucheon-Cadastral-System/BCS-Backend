package com.is.bcs.application.port.out.controlpoint;

import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.PointType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadControlPointPort {

    Optional<ControlPoint> findById(Long id);

    Optional<ControlPoint> findByPointNo(String pointNo);

    List<ControlPoint> findAll();

    boolean existsByPointNo(String pointNo);

    long count();

    /** 종류별 개수 — 저장된 종류만 키로 담는다(집계는 DB가 한다). */
    Map<PointType, Long> countByType();
}
