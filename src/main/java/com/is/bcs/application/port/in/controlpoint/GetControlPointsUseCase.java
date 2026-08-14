package com.is.bcs.application.port.in.controlpoint;

import com.is.bcs.application.dto.ControlPointCountSummary;
import com.is.bcs.application.dto.LastSurveySummary;
import com.is.bcs.application.dto.PointLastSurvey;
import com.is.bcs.domain.controlpoint.ControlPoint;

import java.util.List;

public interface GetControlPointsUseCase {

    List<ControlPoint> getAll();

    ControlPoint getByPointNo(String pointNo);

    ControlPointCountSummary getCountSummary();

    /**
     * 이 기준점의 최종조사 요약(결과·조사일·조사원). 조사한 적이 없으면 세 칸이 비어 있다.
     * 목록에는 싣지 않는다. 점 하나를 고른 뒤에만 필요한 값이라 그때 따로 읽는다.
     */
    LastSurveySummary getLastSurvey(Long pointId);

    /**
     * 조사한 적이 있는 점의 최종조사 결과·조사일. 조사한 적이 없는 점은 담기지 않는다.
     *
     * <p>{@link #getLastSurvey} 를 점 수만큼 부른 것과 답이 같되, 지도가 색을 고를 두 칸만 싣는다.
     * 화면은 조사 프로젝트를 고르지 않고도 점마다 지금 어떤 상태인지 보려고 이것을 읽는다.
     */
    List<PointLastSurvey> getLastSurveys();
}
