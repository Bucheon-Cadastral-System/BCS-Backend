package com.is.bcs.application.port.in.imports;

import com.is.bcs.application.dto.SeedControlPointsCommand;
import com.is.bcs.application.dto.SeedControlPointsResult;

/**
 * 기동 시 기준점 초기 데이터를 넣는다 — 기준점이 하나라도 있으면 아무것도 하지 않는다.
 * 넣을지 판단부터 저장까지 흐름 전체가 여기 있다. 어댑터가 판단·저장을 나눠 가지면 흐름의 절반이 밖에 남는다.
 */
public interface SeedControlPointsUseCase {

    SeedControlPointsResult seedIfEmpty(SeedControlPointsCommand command);
}
