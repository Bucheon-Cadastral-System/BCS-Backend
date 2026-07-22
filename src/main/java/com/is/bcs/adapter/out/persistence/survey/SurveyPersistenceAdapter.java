package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.application.port.out.survey.LoadSurveyProjectPort;
import com.is.bcs.application.port.out.survey.LoadSurveyRecordPort;
import com.is.bcs.application.port.out.survey.SaveSurveyProjectPort;
import com.is.bcs.application.port.out.survey.SaveSurveyRecordPort;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SurveyPersistenceAdapter
        implements LoadSurveyProjectPort, SaveSurveyProjectPort, LoadSurveyRecordPort, SaveSurveyRecordPort {

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
    public List<SurveyRecord> findRecordsByProjectId(Long projectId) {
        return recordRepository.findByProjectId(projectId).stream().map(SurveyRecordJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<SurveyRecord> findRecordByProjectIdAndPointId(Long projectId, Long pointId) {
        return recordRepository.findByProjectIdAndPointId(projectId, pointId).map(SurveyRecordJpaEntity::toDomain);
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
}
