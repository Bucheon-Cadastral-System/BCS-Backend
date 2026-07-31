package com.is.bcs.application.port.in.imports;

import com.is.bcs.application.dto.SurveyCsvPreviewResult;

/** 대상지 파일을 등록하지 않고 읽어만 본다 — 확정 전에 결과를 확인하는 용도. */
public interface PreviewSurveyCsvUseCase {

    SurveyCsvPreviewResult preview(byte[] content, java.util.Map<String, String> columnOverrides);
}
