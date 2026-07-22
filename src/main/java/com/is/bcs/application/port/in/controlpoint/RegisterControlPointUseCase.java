package com.is.bcs.application.port.in.controlpoint;

import com.is.bcs.application.dto.RegisterControlPointCommand;
import com.is.bcs.domain.controlpoint.ControlPoint;

public interface RegisterControlPointUseCase {

    ControlPoint register(RegisterControlPointCommand command);
}
