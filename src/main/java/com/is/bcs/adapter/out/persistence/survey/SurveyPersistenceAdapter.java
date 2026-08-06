package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.application.port.out.survey.DeleteSurveyProjectPort;
import com.is.bcs.application.port.out.survey.DeleteSurveyRecordPort;
import com.is.bcs.application.port.out.survey.LoadSurveyProjectPort;
import com.is.bcs.application.port.out.survey.LoadSurveyRecordPort;
import com.is.bcs.application.port.out.survey.SaveSurveyProjectPort;
import com.is.bcs.application.port.out.survey.SaveSurveyRecordPort;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
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
        return projectRepository.save(SurveyProjectJpaEntity.fromDomain(project)).toDomain();
    }

    @Override
    public void deleteProjectById(Long id) {
        projectRepository.deleteById(id);
    }

    @Override
    public List<SurveyRecord> findRecordsByProjectId(Long projectId) {
        return recordRepository.findByProjectId(projectId).stream().map(SurveyRecordJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<SurveyRecord> findRecordByProjectIdAndPointId(Long projectId, Long pointId) {
        return recordRepository.findByProjectIdAndPointId(projectId, pointId).map(SurveyRecordJpaEntity::toDomain);
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
        return recordRepository.save(SurveyRecordJpaEntity.fromDomain(record)).toDomain();
    }

    @Override
    public List<SurveyRecord> saveAll(List<SurveyRecord> records) {
        List<SurveyRecordJpaEntity> entities = records.stream().map(SurveyRecordJpaEntity::fromDomain).toList();
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
        // 문장은 반영 행 수만 돌려준다 — id·created_at 은 같은 트랜잭션에서 되읽는다
        return recordRepository.findByProjectIdAndPointId(record.getProjectId(), record.getPointId())
                .map(SurveyRecordJpaEntity::toDomain);
    }

    @Override
    public boolean existsRecordByPointId(Long pointId) {
        return recordRepository.existsByPointId(pointId);
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
        recordRepository.deleteByProjectIdAndPointId(projectId, pointId);
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        recordRepository.deleteByProjectId(projectId);
    }

    @Override
    public void deleteByProjectIdAndPointIds(Long projectId, List<Long> pointIds) {
        // PostgreSQL 바인드 변수 상한(65,535)에 여유를 두고 나눈다 — 점 조회 어댑터와 같은 규칙
        for (int from = 0; from < pointIds.size(); from += CHUNK_SIZE) {
            recordRepository.deleteByProjectIdAndPointIdIn(
                    projectId, pointIds.subList(from, Math.min(from + CHUNK_SIZE, pointIds.size())));
        }
    }

    private static final int CHUNK_SIZE = 1_000;
}
