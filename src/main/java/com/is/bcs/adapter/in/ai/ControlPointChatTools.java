package com.is.bcs.adapter.in.ai;

import com.is.bcs.application.port.in.controlpoint.GetControlPointsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 기준점 챗봇 조회 도구(인바운드 어댑터) — 모델이 도구 호출로 데이터를 묻는 진입점.
 * 조회 유스케이스에 위임만 하고 모델용 표현 변환은 응답 record의 from이 맡는다.
 * 쓰기 유스케이스는 등록하지 않고, 조회 실패 예외는 ChatToolErrorProcessor가 모델용 안내로 바꾼다.
 */
@Component
@RequiredArgsConstructor
public class ControlPointChatTools {

    private final GetControlPointsUseCase getControlPointsUseCase;

    @Tool(description = "부천시 지적기준점의 전체 개수(total)와 종류별 개수(countByType: 지적삼각점·지적삼각보조점·지적도근점, 키는 한글 종류명)를 집계해 반환한다. '기준점 총 몇 개', '도근점 개수' 같은 개수·통계 질문에 사용한다")
    public PointCountSummary countControlPoints() {
        return PointCountSummary.from(getControlPointsUseCase.getCountSummary());
    }

    @Tool(description = "관리번호(pointNo)로 지적기준점 1점의 상세를 조회한다. 반환: 관리번호·이름·종류·성과좌표계(crs)·TM 성과좌표(northing=북, easting=동)·경위도(longitude·latitude)·소재지(regionName·address)·표석재질·설치구분·설치일자. 특정 기준점 1점의 좌표·소재지 등을 물을 때 사용한다")
    public ControlPointDetail getControlPointByNo(
            @ToolParam(description = "기준점 관리번호 15자리(예: 41192D000001265)") String pointNo) {
        return ControlPointDetail.from(getControlPointsUseCase.getByPointNo(pointNo));
    }
}
