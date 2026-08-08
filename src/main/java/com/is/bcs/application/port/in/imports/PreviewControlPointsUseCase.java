package com.is.bcs.application.port.in.imports;

import com.is.bcs.application.dto.ControlPointPreviewResult;

/** 기준점 파일을 등록하지 않고 읽어만 본다. */
public interface PreviewControlPointsUseCase {

    ControlPointPreviewResult previewControlPoints(byte[] content);
}
