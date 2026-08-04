package com.is.bcs.application.port.in.imports;

import com.is.bcs.application.dto.ControlPointSeedResult;

/** 기동 시 기준점 초기 데이터를 넣는다 — 읽히는 행만 등록하고 나머지는 건너뛴다. */
public interface SeedControlPointsUseCase {

    ControlPointSeedResult seed(byte[] content);
}
