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
import org.springframework.stereotype.Component;

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

    @Override
    public Optional<SurveyProject> findProjectById(Long id) {
        return projectRepository.findById(id).map(SurveyProjectJpaEntity::toDomain);
    }

    @Override
    public List<SurveyProject> findAllProjects() {
        return projectRepository.findAll().stream().map(SurveyProjectJpaEntity::toDomain).toList();
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
    public boolean existsRecordByPointId(Long pointId) {
        return recordRepository.existsByPointId(pointId);
    }

    @Override
    public void deleteByProjectIdAndPointId(Long projectId, Long pointId) {
        recordRepository.deleteByProjectIdAndPointId(projectId, pointId);
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        recordRepository.deleteByProjectId(projectId);
    }
}
