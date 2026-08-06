package com.is.bcs.application.port.in.controlpointimage;

import com.is.bcs.application.dto.ControlPointImageFileResult;

public interface GetControlPointImageFileUseCase {

    ControlPointImageFileResult getFile(Long imageId, Long requesterId);

}