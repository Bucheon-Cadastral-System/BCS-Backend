package com.is.bcs.application.port.in.controlpoint;

import com.is.bcs.application.dto.RegisterControlPointCommand;
import com.is.bcs.application.dto.RegisterControlPointResult;

/** 기준점 한 점 등록 — 파일 임포트와 같은 규칙(이름·종류 매칭, 있으면 갱신)을 쓴다. */
public interface RegisterControlPointUseCase {

    RegisterControlPointResult register(RegisterControlPointCommand command);
}
