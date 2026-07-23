package com.is.bcs.application.port.in.controlpoint;

import com.is.bcs.application.dto.ControlPointCountSummary;
import com.is.bcs.domain.controlpoint.ControlPoint;

import java.util.List;

public interface GetControlPointsUseCase {

    List<ControlPoint> getAll();

    ControlPoint getByPointNo(String pointNo);

    ControlPointCountSummary getCountSummary();
}
