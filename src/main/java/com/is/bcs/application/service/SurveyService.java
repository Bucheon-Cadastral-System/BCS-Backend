package com.is.bcs.application.service;

import com.is.bcs.application.dto.CreateSurveyProjectCommand;
import com.is.bcs.application.dto.RecordSurveyCommand;
import com.is.bcs.application.dto.SurveyProgress;
import com.is.bcs.application.port.in.survey.CancelSurveyUseCase;
import com.is.bcs.application.port.in.survey.CreateSurveyProjectUseCase;
import com.is.bcs.application.port.in.survey.GetSurveyProjectsUseCase;
import com.is.bcs.application.port.in.survey.GetSurveyRecordsUseCase;
import com.is.bcs.application.port.in.survey.RecordSurveyUseCase;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.survey.DeleteSurveyRecordPort;
import com.is.bcs.application.port.out.survey.LoadSurveyProjectPort;
import com.is.bcs.application.port.out.survey.LoadSurveyRecordPort;
import com.is.bcs.application.port.out.survey.LoadSurveyTargetPort;
import com.is.bcs.application.port.out.survey.SaveSurveyProjectPort;
import com.is.bcs.application.port.out.survey.SaveSurveyRecordPort;
import com.is.bcs.domain.controlpoint.exception.ControlPointNotFoundException;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import com.is.bcs.domain.survey.exception.SurveyProjectNotFoundException;
import com.is.bcs.domain.survey.exception.SurveyRecordNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class SurveyService implements CreateSurveyProjectUseCase, GetSurveyProjectsUseCase,
        RecordSurveyUseCase, CancelSurveyUseCase, GetSurveyRecordsUseCase {

    private final LoadSurveyProjectPort loadSurveyProjectPort;
    private final SaveSurveyProjectPort saveSurveyProjectPort;
    private final LoadSurveyRecordPort loadSurveyRecordPort;
    private final SaveSurveyRecordPort saveSurveyRecordPort;
    private final DeleteSurveyRecordPort deleteSurveyRecordPort;
    private final LoadSurveyTargetPort loadSurveyTargetPort;
    private final LoadControlPointPort loadControlPointPort;
    private final Clock clock;

    @Override
    public SurveyProject create(CreateSurveyProjectCommand command) {
        return saveSurveyProjectPort.save(
                SurveyProject.create(command.name(), command.startedOn(), command.endedOn(), command.note()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurveyProject> getAll() {
        return loadSurveyProjectPort.findAllProjects();
    }

    @Override
    @Transactional(readOnly = true)
    public SurveyProject getById(Long id) {
        return requireProject(id);
    }

    /** 조사 수행 기록 — 이미 조사한 점이면 새 레코드가 아니라 판정 정정(revise)으로 처리한다. */
    @Override
    public SurveyRecord record(RecordSurveyCommand command) {
        requireProject(command.projectId());
        requirePoint(command.pointId());

        OffsetDateTime now = OffsetDateTime.now(clock);
        SurveyRecord record = loadSurveyRecordPort
                .findRecordByProjectIdAndPointId(command.projectId(), command.pointId())
                .map(existing -> {
                    existing.revise(command.result(), now, command.note());
                    return existing;
                })
                .orElseGet(() -> SurveyRecord.create(
                        command.projectId(), command.pointId(), command.result(), now, command.note()));

        return saveSurveyRecordPort.save(record);
    }

    @Override
    public void cancel(Long projectId, Long pointId) {
        loadSurveyRecordPort.findRecordByProjectIdAndPointId(projectId, pointId)
                .orElseThrow(() -> new SurveyRecordNotFoundException(
                        "조사기록을 찾을 수 없습니다: 프로젝트 " + projectId + ", 기준점 " + pointId));

        deleteSurveyRecordPort.deleteByProjectIdAndPointId(projectId, pointId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurveyRecord> getByProjectId(Long projectId) {
        requireProject(projectId);
        return loadSurveyRecordPort.findRecordsByProjectId(projectId);
    }

    /** 프로젝트의 조사 대상 점 id — 없는 프로젝트면 거부한다. */
    @Override
    @Transactional(readOnly = true)
    public List<Long> getTargetPointIds(Long projectId) {
        requireProject(projectId);
        return loadSurveyTargetPort.findPointIdsByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public SurveyProgress getProgress(Long projectId) {
        SurveyProject project = requireProject(projectId);

        Map<SurveyResult, Long> stored = loadSurveyRecordPort.countByResult(projectId);
        Map<SurveyResult, Long> countByResult = new LinkedHashMap<>();
        long surveyed = 0;
        for (SurveyResult result : SurveyResult.values()) {
            long count = stored.getOrDefault(result, 0L);
            countByResult.put(result, count);
            surveyed += count;
        }

        long totalPoints = loadSurveyTargetPort.countByProjectId(projectId);
        long notSurveyedPoints = totalPoints - surveyed;
        boolean complete = totalPoints > 0 && notSurveyedPoints == 0;
        return new SurveyProgress(
                project.getName(), totalPoints, surveyed, notSurveyedPoints, complete, countByResult);
    }

    private SurveyProject requireProject(Long id) {
        return loadSurveyProjectPort.findProjectById(id)
                .orElseThrow(() -> new SurveyProjectNotFoundException(
                        "조사 프로젝트를 찾을 수 없습니다: " + id));
    }

    private void requirePoint(Long id) {
        loadControlPointPort.findById(id)
                .orElseThrow(() -> new ControlPointNotFoundException(
                        "기준점을 찾을 수 없습니다: " + id));
    }
}
