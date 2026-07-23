package com.is.bcs.adapter.out.persistence.controlpoint;

import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.PointType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ControlPointPersistenceAdapter
        implements LoadControlPointPort, SaveControlPointPort {

    private final ControlPointJpaRepository repository;

    @Override
    public Optional<ControlPoint> findById(Long id) {
        return repository.findById(id).map(ControlPointJpaEntity::toDomain);
    }

    @Override
    public Optional<ControlPoint> findByPointNo(String pointNo) {
        return repository.findByPointNo(pointNo).map(ControlPointJpaEntity::toDomain);
    }

    @Override
    public Optional<ControlPoint> findByNameAndType(String name, PointType type) {
        return repository.findFirstByNameAndType(name, type).map(ControlPointJpaEntity::toDomain);
    }

    @Override
    public List<ControlPoint> findAll() {
        return repository.findAll().stream().map(ControlPointJpaEntity::toDomain).toList();
    }

    @Override
    public boolean existsByPointNo(String pointNo) {
        return repository.existsByPointNo(pointNo);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public Map<PointType, Long> countByType() {
        return repository.countByType().stream()
                .collect(Collectors.toMap(
                        ControlPointJpaRepository.PointTypeCount::getType,
                        ControlPointJpaRepository.PointTypeCount::getCount));
    }

    @Override
    public ControlPoint save(ControlPoint point) {
        return repository.save(ControlPointJpaEntity.fromDomain(point)).toDomain();
    }

    @Override
    public List<ControlPoint> saveAll(List<ControlPoint> points) {
        List<ControlPointJpaEntity> entities = points.stream().map(ControlPointJpaEntity::fromDomain).toList();
        return repository.saveAll(entities).stream().map(ControlPointJpaEntity::toDomain).toList();
    }
}
