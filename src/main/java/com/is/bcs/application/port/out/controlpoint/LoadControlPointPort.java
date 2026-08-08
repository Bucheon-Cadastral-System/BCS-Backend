package com.is.bcs.application.port.out.controlpoint;

import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.PointType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadControlPointPort {

    Optional<ControlPoint> findById(Long id);

    Optional<ControlPoint> findByPointNo(String pointNo);

    /** 이름·종류로 조회 — 임포트 시 관리번호가 달라도 같은 물리적 점을 찾아 중복 등록을 막는다(부천 도근점은 이름 유일). */
    Optional<ControlPoint> findByNameAndType(String name, PointType type);

    /**
     * 임포트가 맞춰 볼 기존 점 — 이름이 겹치는 점(같은 점 찾기)과 관리번호가 겹치는 점(충돌 확인)을 한 번에 읽는다.
     * 행마다 찾으면 질의가 파일 크기만큼 늘어난다.
     */
    List<ControlPoint> findAllByNameInOrPointNoIn(Collection<String> names, Collection<String> pointNos);

    /** id 목록 일괄 조회 — 대상 지정처럼 여러 점을 한 번에 검증할 때 쓴다(행마다 찾으면 질의가 목록 크기만큼 늘어난다). */
    List<ControlPoint> findAllByIds(Collection<Long> ids);

    List<ControlPoint> findAll();

    boolean existsByPointNo(String pointNo);

    long count();

    /** 종류별 개수 — 저장된 종류만 키로 담는다(집계는 DB가 한다). */
    Map<PointType, Long> countByType();
}
