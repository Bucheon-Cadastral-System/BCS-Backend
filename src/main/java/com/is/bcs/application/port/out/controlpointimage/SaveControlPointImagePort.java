package com.is.bcs.application.port.out.controlpointimage;

import com.is.bcs.domain.controlpointimage.ControlPointImage;

public interface SaveControlPointImagePort {

    ControlPointImage save(ControlPointImage image);

}