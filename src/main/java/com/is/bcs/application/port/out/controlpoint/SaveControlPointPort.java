package com.is.bcs.application.port.out.controlpoint;

import com.is.bcs.domain.controlpoint.ControlPoint;

import java.util.List;

public interface SaveControlPointPort {

    ControlPoint save(ControlPoint point);

    List<ControlPoint> saveAll(List<ControlPoint> points);
}
