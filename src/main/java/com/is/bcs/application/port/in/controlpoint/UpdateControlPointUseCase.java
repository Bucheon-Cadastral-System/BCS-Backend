package com.is.bcs.application.port.in.controlpoint;

import com.is.bcs.application.dto.UpdateControlPointCommand;
import com.is.bcs.application.dto.UpdateControlPointResult;

/** 기준점 수정 — id 로 지목한 점의 식별·성과를 바꾼다(관리번호·이름이 바뀔 수 있어 경로 식별자는 id 다). */
public interface UpdateControlPointUseCase {

    UpdateControlPointResult update(UpdateControlPointCommand command);
}
