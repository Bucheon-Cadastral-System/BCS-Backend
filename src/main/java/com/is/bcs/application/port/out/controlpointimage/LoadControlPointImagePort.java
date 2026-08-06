package com.is.bcs.application.port.out.controlpointimage;

import com.is.bcs.domain.controlpointimage.ControlPointImage;

import java.util.List;
import java.util.Optional;

public interface LoadControlPointImagePort {

    Optional<ControlPointImage> findById(Long imageId);

    Optional<ControlPointImage> findByProjectIdAndPointId(Long projectId, Long pointId);

    List<ControlPointImage> findAllByPointId(Long pointId);

    boolean existsByProjectIdAndPointId(Long projectId, Long pointId);

}