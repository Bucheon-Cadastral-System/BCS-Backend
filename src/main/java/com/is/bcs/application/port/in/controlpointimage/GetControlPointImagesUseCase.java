package com.is.bcs.application.port.in.controlpointimage;

import com.is.bcs.domain.controlpointimage.ControlPointImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetControlPointImagesUseCase {

    Page<ControlPointImage> getByPointId(Long pointId, Long requesterId, Pageable pageable);

    Page<ControlPointImage> getByProjectId(Long projectId, Long requesterId, Pageable pageable);

    Page<ControlPointImage> getAll(Long requesterId, Pageable pageable);

}