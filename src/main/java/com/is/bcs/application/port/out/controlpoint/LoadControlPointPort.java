package com.is.bcs.application.port.out.controlpoint;

import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.PointType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadControlPointPort {

    Optional<ControlPoint> findById(Long id);

    Optional<ControlPoint> findByPointNo(String pointNo);

    /** 이름·종류로 조회 — 임포트 시 관리번호가 달라도 같은 물리적 점을 찾아 중복 등록을 막는다(부천 도근점은 이름 유일). */
    Optional<ControlPoint> findByNameAndType(String name, PointType type);

    List<ControlPoint> findAll();

    boolean existsByPointNo(String pointNo);

    long count();

    /** 종류별 개수 — 저장된 종류만 키로 담는다(집계는 DB가 한다). */
    Map<PointType, Long> countByType();
}
