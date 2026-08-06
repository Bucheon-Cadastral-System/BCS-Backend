package com.is.bcs.adapter.out.persistence.controlpointimage;

import com.is.bcs.application.port.out.controlpointimage.DeleteControlPointImagePort;
import com.is.bcs.application.port.out.controlpointimage.LoadControlPointImagePort;
import com.is.bcs.application.port.out.controlpointimage.SaveControlPointImagePort;
import com.is.bcs.domain.controlpointimage.ControlPointImage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ControlPointImagePersistenceAdapter
        implements LoadControlPointImagePort,
        SaveControlPointImagePort,
        DeleteControlPointImagePort {

    private final ControlPointImageJpaRepository repository;

    @Override
    public Optional<ControlPointImage> findById(Long imageId) {
        return repository.findById(imageId)
                .map(controlPointImageJpaEntity -> controlPointImageJpaEntity.toDomain());
    }

    @Override
    public Optional<ControlPointImage> findByProjectIdAndPointId(Long projectId, Long pointId) {
        return repository.findByProjectIdAndPointId(projectId, pointId)
                .map(controlPointImageJpaEntity -> controlPointImageJpaEntity.toDomain());
    }

    @Override
    public Page<ControlPointImage> findAllByPointId(Long pointId, Pageable pageable) {
        return repository
                .findAllByPointId(pointId, pageable)
                .map(controlPointImageJpaEntity -> controlPointImageJpaEntity.toDomain());
    }

    @Override
    public Page<ControlPointImage> findAllByProjectId(Long projectId, Pageable pageable) {
        return repository
                .findAllByProjectId(projectId, pageable)
                .map(controlPointImageJpaEntity -> controlPointImageJpaEntity.toDomain());
    }

    @Override
    public Page<ControlPointImage> findAll(Pageable pageable) {
        return repository
                .findAll(pageable)
                .map(controlPointImageJpaEntity -> controlPointImageJpaEntity.toDomain());
    }

    @Override
    public boolean existsByProjectIdAndPointId(Long projectId, Long pointId) {
        return repository.existsByProjectIdAndPointId(projectId, pointId);
    }

    @Override
    public ControlPointImage save(ControlPointImage image) {
        ControlPointImageJpaEntity entity = ControlPointImageJpaEntity.fromDomain(image);

        ControlPointImageJpaEntity saved = repository.saveAndFlush(entity);

        return saved.toDomain();
    }

    @Override
    public void deleteByIdAndFlush(Long imageId) {
        repository.deleteById(imageId);
        repository.flush();
    }

}