package com.is.bcs.application.port.in.controlpoint;

import com.is.bcs.application.dto.ControlPointCountSummary;
import com.is.bcs.domain.controlpoint.ControlPoint;

import java.util.List;

public interface GetControlPointsUseCase {

    List<ControlPoint> getAll();

    ControlPoint getByPointNo(String pointNo);

    ControlPointCountSummary getCountSummary();

    /**
     * 이 기준점을 마지막으로 조사한 사람의 표시명. 기록이 없거나 인증 없이 남긴 기록이면 null.
     * 목록에는 싣지 않는다. 점 하나를 고른 뒤에만 필요한 값이라 그때 따로 읽는다.
     */
    String getLastSurveyorName(Long pointId);
}
