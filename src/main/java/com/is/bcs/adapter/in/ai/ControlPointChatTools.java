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

    @Tool(description = "지적기준점 전체 개수와 종류별(지적삼각점·지적삼각보조점·지적도근점) 개수를 조회한다.")
    public PointCountSummary countControlPoints() {
        return PointCountSummary.from(getControlPointsUseCase.getCountSummary());
    }

    @Tool(description = "관리번호로 지적기준점 1점의 상세(이름·종류·성과좌표·경위도·소재지·설치 정보)를 조회한다.")
    public ControlPointDetail getControlPointByNo(
            @ToolParam(description = "기준점 관리번호(예: 41192D000001265)") String pointNo) {
        return ControlPointDetail.from(getControlPointsUseCase.getByPointNo(pointNo));
    }
}
