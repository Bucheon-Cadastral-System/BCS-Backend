package com.is.bcs.application.service;

import com.is.bcs.application.dto.CreateSurveyProjectCommand;
import com.is.bcs.application.dto.RecordSurveyCommand;
import com.is.bcs.application.dto.SurveyProgress;
import com.is.bcs.application.dto.SurveyProjectSummary;
import com.is.bcs.application.dto.SurveyRecordSummary;
import com.is.bcs.application.dto.UpdateSurveyProjectCommand;
import com.is.bcs.application.port.out.member.LoadMemberNamesPort;
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
import com.is.bcs.domain.survey.exception.SurveyTargetNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final LoadMemberNamesPort loadMemberNamesPort;
    private final Clock clock;

    /**
     * 프로젝트는 점을 지정해 조사 여부를 적는 단위라 대상 없이 만들 수 없다 —
     * 파일 등록은 파일의 행이, 이 경로는 명시한 점 목록이 대상이 된다.
     */
    @Override
    public SurveyProject create(CreateSurveyProjectCommand command) {
        List<Long> pointIds = requireTargetPoints(command.targetPointIds());
        SurveyProject project = saveSurveyProjectPort.save(SurveyProject.create(
                command.authorId(), command.name(), command.startedOn(), command.endedOn(), command.note()));
        saveSurveyTargetPort.saveAll(pointIds.stream()
                .map(pointId -> SurveyTarget.create(project.getId(), pointId))
                .toList());
        return project;
    }

    /**
     * 수정은 값 갱신과 대상 재지정을 함께 다룬다 — 대상 목록은 부분 수정이 아니라 수정 후의 전체를 다시 적는 값이다.
     * 대상에서 빠진 점의 조사 기록은 함께 지운다: 기록은 그 프로젝트가 그 점을 조사하기로 한 데 딸린 데이터라,
     * 연결만 끊어 남기면 어느 화면에도 닿지 않으면서 기준점 삭제만 막는 주인 없는 행이 된다(프로젝트 삭제와 같은 원칙).
     */
    @Override
    public SurveyProject update(UpdateSurveyProjectCommand command) {
        SurveyProject project = requireProject(command.projectId());
        // 값 대입 전에 검증을 전부 끝낸다 — 거부된 수정이 일부만 반영된 채 남지 않게(update 의 원자성과 같은 결)
        List<Long> pointIds = requireTargetPoints(command.targetPointIds());
        project.update(command.name(), command.startedOn(), command.endedOn(), command.note());
        SurveyProject saved = saveSurveyProjectPort.save(project);

        Set<Long> current = Set.copyOf(loadSurveyTargetPort.findPointIdsByProjectId(saved.getId()));
        Set<Long> requested = Set.copyOf(pointIds);
        List<Long> removed = current.stream().filter(id -> !requested.contains(id)).toList();
        if (!removed.isEmpty()) {
            // 프로젝트 삭제와 같은 순서(기록→대상) — 대상이 먼저 사라지면 기록이 잠깐 비대상 상태로 남는다
            deleteSurveyRecordPort.deleteByProjectIdAndPointIds(saved.getId(), removed);
            deleteSurveyTargetPort.deleteByProjectIdAndPointIds(saved.getId(), removed);
        }
        List<SurveyTarget> added = pointIds.stream()
                .filter(id -> !current.contains(id))
                .map(id -> SurveyTarget.create(saved.getId(), id))
                .toList();
        if (!added.isEmpty()) {
            saveSurveyTargetPort.saveAll(added);
        }
        return saved;
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
        // null 요소는 값이 오지 않은 것 — 흘려보내면 조회 단계에서 서버 오류(5xx)로 둔갑한다
        if (targetPointIds == null || targetPointIds.isEmpty()
                || targetPointIds.stream().anyMatch(Objects::isNull)) {
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

    /** 목록 요약 — 행별 진행률 조회(N+1) 대신 프로젝트·대상·조사 수를 각각 한 번에 모아 붙인다. */
    @Override
    @Transactional(readOnly = true)
    public List<SurveyProjectSummary> getSummaries() {
        List<SurveyProject> projects = loadSurveyProjectPort.findAllProjects();
        Map<Long, Long> targetCounts = loadSurveyTargetPort.countTargetsByProject();
        Map<Long, Long> surveyedCounts = loadSurveyRecordPort.countSurveyedByProject();
        Map<Long, String> authorNames = loadMemberNamesPort.findNamesByIds(projects.stream()
                .map(SurveyProject::getAuthorId).filter(Objects::nonNull).collect(Collectors.toSet()));
        return projects.stream()
                .map(project -> new SurveyProjectSummary(
                        project,
                        targetCounts.getOrDefault(project.getId(), 0L),
                        surveyedCounts.getOrDefault(project.getId(), 0L),
                        project.getAuthorId() == null ? null : authorNames.get(project.getAuthorId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SurveyProject getById(Long id) {
        return requireProject(id);
    }

    /**
     * 조사 수행 기록 — 이미 조사한 점이면 새 레코드가 아니라 판정 정정으로 처리한다.
     * 대상 확인·삽입·정정은 영속 계층의 원자 한 문장이다 — 확인과 쓰기를 가르면
     * 동시 기록이 중복 행을, 동시 대상 재지정이 대상 아닌 기록을 그 틈에 남긴다.
     * 프로젝트·기준점 사전 확인은 실패 사유를 404 로 정확히 가르는 안내용으로 남긴다.
     */
    @Override
    public SurveyRecordSummary record(RecordSurveyCommand command) {
        requireProject(command.projectId());
        requirePoint(command.pointId());

        SurveyRecord written = saveSurveyRecordPort.upsertForTarget(SurveyRecord.create(
                        command.projectId(), command.pointId(), command.result(),
                        OffsetDateTime.now(clock), command.note(), command.surveyorId()))
                // 기록은 대상으로 지정한 점에만 — 비대상 기록을 허용하면 화면 밖 경로(직접 호출)로 진행률의 전제가 깨진다
                .orElseThrow(() -> new SurveyTargetNotFoundException(
                        "프로젝트의 조사 대상이 아닌 기준점입니다: " + command.pointId()));
        return withSurveyorName(written);
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
    public List<SurveyRecordSummary> getByProjectId(Long projectId) {
        requireProject(projectId);
        List<SurveyRecord> records = loadSurveyRecordPort.findRecordsByProjectId(projectId);
        Map<Long, String> names = loadMemberNamesPort.findNamesByIds(records.stream()
                .map(SurveyRecord::getSurveyedById).filter(Objects::nonNull).collect(Collectors.toSet()));
        return records.stream()
                .map(record -> new SurveyRecordSummary(
                        record, record.getSurveyedById() == null ? null : names.get(record.getSurveyedById())))
                .toList();
    }

    /** 한 건에 조사원 이름을 붙인다 — 기록 응답만으로 화면이 이름을 그릴 수 있게. */
    private SurveyRecordSummary withSurveyorName(SurveyRecord record) {
        if (record.getSurveyedById() == null) {
            return new SurveyRecordSummary(record, null);
        }
        return new SurveyRecordSummary(record,
                loadMemberNamesPort.findNamesByIds(Set.of(record.getSurveyedById())).get(record.getSurveyedById()));
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
