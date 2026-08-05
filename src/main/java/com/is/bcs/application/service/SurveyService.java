package com.is.bcs.application.service;

import com.is.bcs.application.dto.CreateSurveyProjectCommand;
import com.is.bcs.application.dto.RecordSurveyCommand;
import com.is.bcs.application.dto.SurveyProgress;
import com.is.bcs.application.dto.UpdateSurveyProjectCommand;
import com.is.bcs.application.port.in.survey.CancelSurveyUseCase;
import com.is.bcs.application.port.in.survey.CreateSurveyProjectUseCase;
import com.is.bcs.application.port.in.survey.DeleteSurveyProjectUseCase;
import com.is.bcs.application.port.in.survey.GetSurveyProjectsUseCase;
import com.is.bcs.application.port.in.survey.GetSurveyRecordsUseCase;
import com.is.bcs.application.port.in.survey.RecordSurveyUseCase;
import com.is.bcs.application.port.in.survey.UpdateSurveyProjectUseCase;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.survey.DeleteSurveyProjectPort;
import com.is.bcs.application.port.out.survey.DeleteSurveyRecordPort;
import com.is.bcs.application.port.out.survey.DeleteSurveyTargetPort;
import com.is.bcs.application.port.out.survey.LoadSurveyProjectPort;
import com.is.bcs.application.port.out.survey.LoadSurveyRecordPort;
import com.is.bcs.application.port.out.survey.LoadSurveyTargetPort;
import com.is.bcs.application.port.out.survey.SaveSurveyProjectPort;
import com.is.bcs.application.port.out.survey.SaveSurveyRecordPort;
import com.is.bcs.application.port.out.survey.SaveSurveyTargetPort;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.exception.ControlPointNotFoundException;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import com.is.bcs.domain.survey.SurveyTarget;
import com.is.bcs.domain.survey.exception.InvalidSurveyException;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SurveyService implements CreateSurveyProjectUseCase, UpdateSurveyProjectUseCase,
        DeleteSurveyProjectUseCase, GetSurveyProjectsUseCase,
        RecordSurveyUseCase, CancelSurveyUseCase, GetSurveyRecordsUseCase {

    private final LoadSurveyProjectPort loadSurveyProjectPort;
    private final SaveSurveyProjectPort saveSurveyProjectPort;
    private final DeleteSurveyProjectPort deleteSurveyProjectPort;
    private final LoadSurveyRecordPort loadSurveyRecordPort;
    private final SaveSurveyRecordPort saveSurveyRecordPort;
    private final DeleteSurveyRecordPort deleteSurveyRecordPort;
    private final LoadSurveyTargetPort loadSurveyTargetPort;
    private final SaveSurveyTargetPort saveSurveyTargetPort;
    private final DeleteSurveyTargetPort deleteSurveyTargetPort;
    private final LoadControlPointPort loadControlPointPort;
    private final Clock clock;

    /**
     * 프로젝트는 점을 지정해 조사 여부를 적는 단위라 대상 없이 만들 수 없다 —
     * 파일 등록은 파일의 행이, 이 경로는 명시한 점 목록이 대상이 된다.
     */
    @Override
    public SurveyProject create(CreateSurveyProjectCommand command) {
        List<Long> pointIds = requireTargetPoints(command.targetPointIds());
        SurveyProject project = saveSurveyProjectPort.save(
                SurveyProject.create(command.name(), command.startedOn(), command.endedOn(), command.note()));
        saveSurveyTargetPort.saveAll(pointIds.stream()
                .map(pointId -> SurveyTarget.create(project.getId(), pointId))
                .toList());
        return project;
    }

    @Override
    public SurveyProject update(UpdateSurveyProjectCommand command) {
        SurveyProject project = requireProject(command.projectId());
        project.update(command.name(), command.startedOn(), command.endedOn(), command.note());
        return saveSurveyProjectPort.save(project);
    }

    /** 대상 지정·조사 기록은 프로젝트에 속한 데이터라 함께 지운다 — 남기면 주인 없는 행이 된다. */
    @Override
    public void delete(Long projectId) {
        requireProject(projectId);
        deleteSurveyRecordPort.deleteByProjectId(projectId);
        deleteSurveyTargetPort.deleteByProjectId(projectId);
        deleteSurveyProjectPort.deleteProjectById(projectId);
    }

    /** 대상 점 검증 — 같은 점을 두 번 적어도 대상은 한 번만 지정되고, 없는 점이 섞여 있으면 전체를 거부한다. */
    private List<Long> requireTargetPoints(List<Long> targetPointIds) {
        if (targetPointIds == null || targetPointIds.isEmpty()) {
            throw new InvalidSurveyException("대상 기준점을 1점 이상 지정해 주세요.");
        }
        List<Long> pointIds = targetPointIds.stream().distinct().toList();
        Set<Long> found = loadControlPointPort.findAllByIds(pointIds).stream()
                .map(ControlPoint::getId)
                .collect(Collectors.toSet());
        List<Long> missing = pointIds.stream().filter(id -> !found.contains(id)).toList();
        if (!missing.isEmpty()) {
            throw new ControlPointNotFoundException("기준점을 찾을 수 없습니다: " + missing.stream()
                    .map(String::valueOf).collect(Collectors.joining(", ")));
        }
        return pointIds;
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
                // 조사원은 인증 주체로 채울 값이라 인증이 붙기 전까지 비워 둔다
                .orElseGet(() -> SurveyRecord.create(
                        command.projectId(), command.pointId(), command.result(), now, command.note(), null));

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
