package com.is.bcs.application.port.in.imports;

import com.is.bcs.application.dto.ControlPointImportResult;

/** 기준점 파일로 기준점 마스터만 등록·갱신한다 — 조사는 만들지 않는다. */
public interface ImportControlPointsUseCase {

    ControlPointImportResult importControlPoints(byte[] content);
}
