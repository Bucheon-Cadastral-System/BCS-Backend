package com.is.bcs.support;

import com.is.bcs.application.port.out.controlpoint.DeleteControlPointPort;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.PointType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 기준점 저장소 페이크 — 저장하면 id를 매기고, id가 있는 점은 그 자리를 덮는다.
 * id 를 매겨야 임포트가 대상·기록을 붙일 점을 되찾을 수 있어, 등록과 갱신을 가르는 동작까지 확인된다.
 */
public class FakeControlPointStore implements LoadControlPointPort, SaveControlPointPort, DeleteControlPointPort {

    public final Map<Long, ControlPoint> points = new HashMap<>();
    private long sequence = 0;

    @Override
    public Optional<ControlPoint> findById(Long id) {
        return Optional.ofNullable(points.get(id));
    }

    @Override
    public List<ControlPoint> findAllByIds(Collection<Long> ids) {
        return ids.stream().flatMap(id -> findById(id).stream()).toList();
    }

    @Override
    public Optional<ControlPoint> findByPointNo(String pointNo) {
        return points.values().stream().filter(p -> p.getPointNo().equals(pointNo)).findFirst();
    }

    @Override
    public Optional<ControlPoint> findByNameAndType(String name, PointType type) {
        return points.values().stream()
                .filter(p -> p.getName().equals(name) && p.getType() == type)
                .findFirst();
    }

    @Override
    public List<ControlPoint> findAllByNameInOrPointNoIn(Collection<String> names, Collection<String> pointNos) {
        return points.values().stream()
                .filter(p -> names.contains(p.getName()) || pointNos.contains(p.getPointNo()))
                .toList();
    }

    @Override
    public List<ControlPoint> findAll() {
        return new ArrayList<>(points.values());
    }

    @Override
    public boolean existsByPointNo(String pointNo) {
        return findByPointNo(pointNo).isPresent();
    }

    @Override
    public long count() {
        return points.size();
    }

    @Override
    public Map<PointType, Long> countByType() {
        Map<PointType, Long> counts = new HashMap<>();
        points.values().forEach(p -> counts.merge(p.getType(), 1L, Long::sum));
        return counts;
    }

    @Override
    public ControlPoint save(ControlPoint point) {
        long id = point.getId() != null ? point.getId() : ++sequence;
        sequence = Math.max(sequence, id); // 복원 점을 저장해도 다음 id가 그 뒤에서 발급되게
        ControlPoint saved = ControlPoint.restore(
                id, point.getPointNo(), point.getType(), point.getName(),
                point.getTm(), point.getGeo(),
                point.getRegionCode(), point.getRegionName(), point.getAddress(),
                point.getMarkerMaterial(), point.getInstallType(), point.getInstalledDate(),
                point.getTraverse(),
                point.getLastSurveyResult(), point.getLastSurveyedOn(),
                point.getVersion()); // 판 번호는 그대로 둔다 — 페이크는 저장을 세지 않는다
        points.put(id, saved);
        return saved;
    }

    @Override
    public List<ControlPoint> saveAll(List<ControlPoint> list) {
        return list.stream().map(this::save).toList();
    }

    @Override
    public void deleteById(Long id) {
        points.remove(id);
    }
}
