package com.is.bcs.application.port.out.controlpointimage;

import com.is.bcs.domain.controlpointimage.ControlPointImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface LoadControlPointImagePort {

    Optional<ControlPointImage> findById(Long imageId);

    Optional<ControlPointImage> findByProjectIdAndPointId(Long projectId, Long pointId);

    Page<ControlPointImage> findAllByPointId(Long pointId, Pageable pageable);

    Page<ControlPointImage> findAllByProjectId(Long projectId, Pageable pageable);

    Page<ControlPointImage> findAll(Pageable pageable);

    boolean existsByProjectIdAndPointId(Long projectId, Long pointId);

}