package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.application.port.out.survey.DeleteSurveyProjectPort;
import com.is.bcs.application.port.out.survey.DeleteSurveyRecordPort;
import com.is.bcs.application.port.out.survey.LoadSurveyProjectPort;
import com.is.bcs.application.port.out.survey.LoadSurveyRecordPort;
import com.is.bcs.application.port.out.survey.SaveSurveyProjectPort;
import com.is.bcs.application.port.out.survey.SaveSurveyRecordPort;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.application.dto.PointLastSurvey;
import com.is.bcs.application.dto.SurveyRecordSummary;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SurveyPersistenceAdapter
        implements LoadSurveyProjectPort, SaveSurveyProjectPort, DeleteSurveyProjectPort,
        LoadSurveyRecordPort, SaveSurveyRecordPort, DeleteSurveyRecordPort {

    // 연관을 껍데기 참조로 만들려면 EntityManager 가 필요하다 — 저장 경로가 상대 행을 읽지 않게 한다
    @PersistenceContext
    private EntityManager entityManager;


    private final SurveyProjectJpaRepository projectRepository;
    private final SurveyRecordJpaRepository recordRepository;
    private final Clock clock; // 감사 시각(created_at·updated_at)용 — 원자 upsert 는 JPA 감사를 거치지 않아 직접 찍는다

    @Override
    public Optional<SurveyProject> findProjectById(Long id) {
        return projectRepository.findById(id).map(SurveyProjectJpaEntity::toDomain);
    }

    @Override
    public List<SurveyProject> findAllProjects() {
        // 최근 시작한 조사가 위 — 화면 정렬(월별·최신순)과 같은 축이고, 무정렬이면 UPDATE 뒤 행 순서가 널뛴다
        return projectRepository.findAll(Sort.by(Sort.Order.desc("startedOn"), Sort.Order.desc("id")))
                .stream().map(SurveyProjectJpaEntity::toDomain).toList();
    }

    @Override
    public SurveyProject save(SurveyProject project) {
        return projectRepository.save(SurveyProjectJpaEntity.fromDomain(project, entityManager)).toDomain();
    }

    @Override
    public void deleteProjectById(Long id) {
        projectRepository.deleteById(id);
    }

    @Override
    public List<SurveyRecord> findRecordsByProjectId(Long projectId) {
        return recordRepository.findByIdProjectId(projectId).stream().map(SurveyRecordJpaEntity::toDomain).toList();
    }

    @Override
    public List<SurveyRecord> findRecordsByPointId(Long pointId) {
        return recordRepository.findByIdPointId(pointId).stream().map(SurveyRecordJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<SurveyRecord> findLatestRecordByPointId(Long pointId) {
        return recordRepository.findLatestByPointId(pointId).map(SurveyRecordJpaEntity::toDomain);
    }

    /**
     * 조사 시각을 조사일로 내리는 자리 — 시각은 순간이고 조사일은 지역의 날짜라 어느 시간대에서 보는지 정해야 한다.
     * 단건 최종조사도 같은 시계로 내리므로 두 경로가 같은 날짜를 낸다.
     */
    @Override
    public List<PointLastSurvey> findLatestSurveyPerPoint() {
        return recordRepository.findLatestSurveyPerPoint().stream()
                .map(latest -> new PointLastSurvey(
                        latest.getPointId(),
                        SurveyResult.valueOf(latest.getResult()),
                        latest.getSurveyedAt().atZone(clock.getZone()).toLocalDate()))
                .toList();
    }

    @Override
    public List<SurveyRecordSummary> findRecordSummariesByProjectId(Long projectId) {
        // 조사원을 조인으로 함께 실어 오므로 이름을 따로 모아 조회하지 않는다
        return recordRepository.findByProjectIdWithSurveyor(projectId).stream()
                .map(entity -> new SurveyRecordSummary(
                        entity.toDomain(),
                        entity.getSurveyor() == null ? null : entity.getSurveyor().getName()))
                .toList();
    }

    @Override
    public Optional<SurveyRecord> findRecordByProjectIdAndPointId(Long projectId, Long pointId) {
        return recordRepository.findById(new ProjectPointId(projectId, pointId)).map(SurveyRecordJpaEntity::toDomain);
    }

    @Override
    public Map<SurveyResult, Long> countByResult(Long projectId) {
        return recordRepository.countByResult(projectId).stream()
                .collect(Collectors.toMap(
                        SurveyRecordJpaRepository.ResultCount::getResult,
                        SurveyRecordJpaRepository.ResultCount::getCount));
    }

    @Override
    public SurveyRecord save(SurveyRecord record) {
        return recordRepository.save(SurveyRecordJpaEntity.fromDomain(record, entityManager)).toDomain();
    }

    @Override
    public List<SurveyRecord> saveAll(List<SurveyRecord> records) {
        List<SurveyRecordJpaEntity> entities = records.stream().map(r -> SurveyRecordJpaEntity.fromDomain(r, entityManager)).toList();
        return recordRepository.saveAll(entities).stream().map(SurveyRecordJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<SurveyRecord> upsertForTarget(SurveyRecord record) {
        int applied = recordRepository.upsertForTarget(
                record.getProjectId(), record.getPointId(), record.getResult().name(),
                record.getSurveyedAt(), record.getNote(), record.getSurveyedById(),
                OffsetDateTime.now(clock));
        if (applied == 0) {
            return Optional.empty(); // 대상이 아니라서 문장이 아무것도 쓰지 않았다
        }
        // 문장은 반영 행 수만 돌려준다 — 확정된 값은 같은 트랜잭션에서 되읽는다.
        // 여기서 비었다는 것은 방금 쓴 행이 사라졌다는 뜻이라 '대상 아님'과 같은 결과로 뭉치지 않는다
        // (뭉치면 쓰기는 커밋된 채 응답만 404 가 된다).
        return Optional.of(recordRepository
                .findById(new ProjectPointId(record.getProjectId(), record.getPointId()))
                .orElseThrow(() -> new IllegalStateException(
                        "기록을 쓴 뒤 되읽지 못했습니다: 프로젝트 " + record.getProjectId()
                                + ", 기준점 " + record.getPointId()))
                .toDomain());
    }

    @Override
    public boolean existsRecordByPointId(Long pointId) {
        return recordRepository.existsByIdPointId(pointId);
    }

    @Override
    public Map<Long, Long> countSurveyedByProject() {
        return recordRepository.countSurveyedByProject().stream()
                .collect(Collectors.toMap(
                        SurveyRecordJpaRepository.ProjectCount::getProjectId,
                        SurveyRecordJpaRepository.ProjectCount::getCnt));
    }

    @Override
    public void deleteByProjectIdAndPointId(Long projectId, Long pointId) {
        recordRepository.deleteByIdProjectIdAndIdPointId(projectId, pointId);
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        recordRepository.deleteByIdProjectId(projectId);
    }

    @Override
    public void deleteByProjectIdAndPointIds(Long projectId, List<Long> pointIds) {
        // PostgreSQL 바인드 변수 상한(65,535)에 여유를 두고 나눈다 — 점 조회 어댑터와 같은 규칙
        for (int from = 0; from < pointIds.size(); from += CHUNK_SIZE) {
            recordRepository.deleteByIdProjectIdAndIdPointIdIn(
                    projectId, pointIds.subList(from, Math.min(from + CHUNK_SIZE, pointIds.size())));
        }
    }

    private static final int CHUNK_SIZE = 1_000;
}
