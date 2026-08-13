package com.is.bcs.application.port.out.controlpoint;

import com.is.bcs.application.dto.PointLastSurvey;
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

    /**
     * 시드 최종조사가 적힌 점만 — 지도가 점마다 최신 상태를 그릴 때 조사기록 쪽과 함께 읽는다.
     *
     * <p>최종조사내용이 빈 행은 담지 않는다. 조사일만 있고 판정이 없으면 무슨 색으로 그릴지 정할 수 없다.
     * 임포트가 표시명으로 맞춰 저장하므로 어휘로 되찾고, 아는 말이 아니면 기타로 싣는다 —
     * 사람이 판정을 적어 둔 이상 조사한 것은 맞으므로 미조사로 셀 수는 없다.
     */
    List<PointLastSurvey> findSeedLastSurveys();
}
