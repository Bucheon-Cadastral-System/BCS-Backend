package com.is.bcs.adapter.out.persistence.controlpoint;

import com.is.bcs.application.dto.PointLastSurvey;
import com.is.bcs.application.port.out.controlpoint.DeleteControlPointPort;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.survey.SurveyResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ControlPointPersistenceAdapter
        implements LoadControlPointPort, SaveControlPointPort, DeleteControlPointPort {

    // 연관을 껍데기 참조로 만들려면 EntityManager 가 필요하다 — 저장 경로가 상대 행을 읽지 않게 한다
    @PersistenceContext
    private EntityManager entityManager;


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

    /**
     * IN 목록은 값 하나가 바인드 변수 하나라, 큰 파일을 통째로 넘기면 드라이버·DB 한도에 걸려 조회 단계에서 실패한다.
     * 나눠 조회하고 id 로 합친다 — 이름과 관리번호 양쪽에 걸린 점이 두 번 담기지 않게.
     */
    @Override
    public List<ControlPoint> findAllByNameInOrPointNoIn(Collection<String> names, Collection<String> pointNos) {
        Map<Long, ControlPointJpaEntity> found = new LinkedHashMap<>();
        forEachChunk(names, chunk -> repository.findAllByNameIn(chunk).forEach(e -> found.put(e.getId(), e)));
        forEachChunk(pointNos, chunk -> repository.findAllByPointNoIn(chunk).forEach(e -> found.put(e.getId(), e)));
        return found.values().stream().map(ControlPointJpaEntity::toDomain).toList();
    }

    @Override
    public List<ControlPoint> findAllByIds(Collection<Long> ids) {
        List<Long> all = List.copyOf(ids);
        List<ControlPoint> found = new ArrayList<>(all.size());
        for (int from = 0; from < all.size(); from += CHUNK_SIZE) {
            repository.findAllById(all.subList(from, Math.min(from + CHUNK_SIZE, all.size())))
                    .forEach(entity -> found.add(entity.toDomain()));
        }
        return found;
    }

    /** PostgreSQL 의 바인드 변수 상한(65,535)에 여유를 두고 나눈다. */
    private static final int CHUNK_SIZE = 1_000;

    private static void forEachChunk(Collection<String> values, Consumer<List<String>> query) {
        List<String> all = List.copyOf(values);
        for (int from = 0; from < all.size(); from += CHUNK_SIZE) {
            query.accept(all.subList(from, Math.min(from + CHUNK_SIZE, all.size())));
        }
    }

    @Override
    public List<ControlPoint> findAll() {
        return repository.findAll().stream().map(ControlPointJpaEntity::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
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

    /**
     * 저장된 문구를 어휘로 되돌린다. 임포트가 "완전"을 정상으로, "망실(포장)"을 망실로 이미 맞춰 두었으므로
     * 표시명 그대로 찾으면 대부분 걸린다. 걸리지 않는 값은 사람이 적어 둔 판정이 어휘 밖에 있는 것이라
     * 기타로 싣는다 — 판정이 적혀 있는 이상 미조사로 셀 수는 없다. 원문은 상세 카드가 그대로 보여 준다.
     */
    @Override
    public List<PointLastSurvey> findSeedLastSurveys() {
        return repository.findSeedLastSurveys().stream()
                .map(seed -> new PointLastSurvey(
                        seed.getPointId(),
                        SurveyResult.fromDisplayName(seed.getResult()).orElse(SurveyResult.ETC),
                        seed.getSurveyedOn()))
                .toList();
    }

    @Override
    public ControlPoint save(ControlPoint point) {
        return repository.save(ControlPointJpaEntity.fromDomain(point, entityManager)).toDomain();
    }

    @Override
    public List<ControlPoint> saveAll(List<ControlPoint> points) {
        List<ControlPointJpaEntity> entities = points.stream().map(p -> ControlPointJpaEntity.fromDomain(p, entityManager)).toList();
        return repository.saveAll(entities).stream().map(ControlPointJpaEntity::toDomain).toList();
    }
}
