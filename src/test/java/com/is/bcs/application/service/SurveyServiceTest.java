package com.is.bcs.application.service;

import com.is.bcs.application.dto.CreateSurveyProjectCommand;
import com.is.bcs.application.dto.PointLastSurvey;
import com.is.bcs.application.dto.RecordSurveyCommand;
import com.is.bcs.application.dto.SurveyProgress;
import com.is.bcs.application.dto.SurveyProjectSummary;
import com.is.bcs.application.dto.SurveyRecordSummary;
import com.is.bcs.application.dto.UpdateSurveyProjectCommand;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.application.port.out.member.LoadMemberNamesPort;
import com.is.bcs.application.port.out.survey.DeleteSurveyProjectPort;
import com.is.bcs.application.port.out.survey.DeleteSurveyRecordPort;
import com.is.bcs.application.port.out.survey.DeleteSurveyTargetPort;
import com.is.bcs.application.port.out.survey.LoadSurveyProjectPort;
import com.is.bcs.application.port.out.survey.LoadSurveyRecordPort;
import com.is.bcs.application.port.out.survey.LoadSurveyTargetPort;
import com.is.bcs.application.port.out.survey.SaveSurveyProjectPort;
import com.is.bcs.application.port.out.survey.SaveSurveyRecordPort;
import com.is.bcs.application.port.out.survey.SaveSurveyTargetPort;
import com.is.bcs.config.TimeConfig;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpoint.exception.ControlPointNotFoundException;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import com.is.bcs.domain.survey.SurveyTarget;
import com.is.bcs.domain.survey.exception.InvalidSurveyException;
import com.is.bcs.domain.survey.exception.SurveyProjectNotFoundException;
import com.is.bcs.domain.survey.exception.SurveyTargetNotFoundException;
import com.is.bcs.domain.survey.exception.SurveyRecordNotFoundException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurveyServiceTest {

    private static final LocalDate STARTED = LocalDate.of(2026, 7, 1);

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-22T09:00:00Z");
    private static final OffsetDateTime FIXED_KST = OffsetDateTime.ofInstant(FIXED_INSTANT, TimeConfig.KST);

    private final FakeTargetStore targetStore = new FakeTargetStore();
    private final FakeSurveyStore store = new FakeSurveyStore(targetStore);
    private final FakePointStore pointStore = new FakePointStore();
    private final FakeMemberNames memberNames = new FakeMemberNames();
    private final SurveyService service = new SurveyService(
            store, store, store, store, store, store, targetStore, targetStore, targetStore,
            pointStore, memberNames, Clock.fixed(FIXED_INSTANT, TimeConfig.KST));

    {
        store.surveyorNames = memberNames.names;
    }

    /** 대상 점을 등록소에 넣고 그 점들을 대상으로 프로젝트를 만든다 — 대상 없는 프로젝트는 만들 수 없다. */
    private SurveyProject sampleProject(Long... targetPointIds) {
        List<Long> ids = targetPointIds.length == 0 ? List.of(1L) : List.of(targetPointIds);
        ids.forEach(pointStore::add);
        return service.create(new CreateSurveyProjectCommand(null, "2026 일제조사", STARTED, null, "정기 조사", ids));
    }

    @Test
    @DisplayName("목록 요약 — 프로젝트마다 대상·조사 수가 실리고, 작성자는 기록이 없으면 null 이다")
    void getSummaries_carriesCountsPerProject() {
        SurveyProject first = sampleProject(10L, 11L);
        service.record(new RecordSurveyCommand(first.getId(), 10L, SurveyResult.INTACT, null, null));
        pointStore.add(12L);
        SurveyProject second = service.create(new CreateSurveyProjectCommand(null, "다른 조사", STARTED, null, null, List.of(12L)));

        Map<Long, SurveyProjectSummary> byId = service.getSummaries().stream()
                .collect(Collectors.toMap(s -> s.project().getId(), s -> s));

        assertEquals(2, byId.size());
        assertEquals(2, byId.get(first.getId()).targetCount());
        assertEquals(1, byId.get(first.getId()).surveyedCount());
        assertEquals(1, byId.get(second.getId()).targetCount());
        assertEquals(0, byId.get(second.getId()).surveyedCount());
        assertNull(byId.get(first.getId()).authorName()); // 인증 없는 호출은 작성자 기록이 없다
    }

    @Test
    @DisplayName("목록 요약 — 인증 주체로 만든 프로젝트는 작성자 이름이 실려 온다")
    void getSummaries_resolvesAuthorName() {
        pointStore.add(10L);
        memberNames.names.put(7L, "김측량");

        SurveyProject project = service.create(
                new CreateSurveyProjectCommand(7L, "일제조사", STARTED, null, null, List.of(10L)));

        SurveyProjectSummary summary = service.getSummaries().stream()
                .filter(s -> s.project().getId().equals(project.getId())).findFirst().orElseThrow();
        assertEquals("김측량", summary.authorName());
    }

    @Test
    @DisplayName("조사 대상 점 id — 그 프로젝트의 대상만 돌려주고 없는 프로젝트는 거부한다")
    void getTargetPointIds() {
        SurveyProject project = sampleProject(10L, 11L);
        pointStore.add(12L);
        service.create(new CreateSurveyProjectCommand(null, "다른 조사", STARTED, null, null, List.of(12L)));

        assertEquals(List.of(10L, 11L), service.getTargetPointIds(project.getId()));
        assertThrows(SurveyProjectNotFoundException.class, () -> service.getTargetPointIds(999L));
    }

    @Test
    @DisplayName("프로젝트를 생성하면 id가 발급되고 조회된다")
    void createAndGetProject() {
        SurveyProject created = sampleProject();

        assertNotNull(created.getId());
        assertEquals("2026 일제조사", service.getById(created.getId()).getName());
        assertEquals(1, service.getAll().size());
    }

    @Test
    @DisplayName("없는 프로젝트 조회는 SurveyProjectNotFoundException")
    void getProject_notFound_throws() {
        assertThrows(SurveyProjectNotFoundException.class, () -> service.getById(99L));
    }

    @Test
    @DisplayName("생성 — 지정한 대상이 저장되고, 같은 점을 두 번 적어도 대상은 한 번만 지정된다")
    void create_savesTargets_deduplicated() {
        pointStore.add(10L);
        pointStore.add(11L);

        SurveyProject project = service.create(new CreateSurveyProjectCommand(null, 
                "일제조사", STARTED, null, null, List.of(10L, 10L, 11L)));

        assertEquals(List.of(10L, 11L), service.getTargetPointIds(project.getId()));
    }

    @Test
    @DisplayName("생성 — 대상이 비어 있으면 거부한다(프로젝트는 점을 지정해 조사 여부를 적는 단위다)")
    void create_withoutTargets_rejected() {
        assertThrows(InvalidSurveyException.class, () -> service.create(
                new CreateSurveyProjectCommand(null, "일제조사", STARTED, null, null, List.of())));
        assertThrows(InvalidSurveyException.class, () -> service.create(
                new CreateSurveyProjectCommand(null, "일제조사", STARTED, null, null, null)));
        // null 요소는 값이 오지 않은 것 — 흘려보내면 조회 단계에서 5xx 로 둔갑한다
        pointStore.add(10L);
        assertThrows(InvalidSurveyException.class, () -> service.create(
                new CreateSurveyProjectCommand(null, "일제조사", STARTED, null, null, Arrays.asList(10L, null))));
        assertTrue(service.getAll().isEmpty());
    }

    @Test
    @DisplayName("생성 — 없는 점이 섞여 있으면 전체를 거부하고 프로젝트도 만들지 않는다")
    void create_missingTargetPoint_rejectedWithoutSaving() {
        pointStore.add(10L);

        ControlPointNotFoundException thrown = assertThrows(ControlPointNotFoundException.class,
                () -> service.create(new CreateSurveyProjectCommand(null, 
                        "일제조사", STARTED, null, null, List.of(10L, 99L))));

        assertTrue(thrown.getMessage().contains("99"));
        assertTrue(service.getAll().isEmpty());
        assertTrue(targetStore.targets.isEmpty());
    }

    @Test
    @DisplayName("수정 — 이름·기간·비고가 바뀌고, 같은 대상을 다시 적으면 대상·기록은 그대로다")
    void update_changesValues_keepsTargets() {
        SurveyProject project = sampleProject(10L);
        service.record(new RecordSurveyCommand(project.getId(), 10L, SurveyResult.INTACT, null, null));

        SurveyProject updated = service.update(new UpdateSurveyProjectCommand(
                project.getId(), " 2026 하반기 조사 ", STARTED.plusDays(1), STARTED.plusDays(30), null, List.of(10L)));

        assertEquals("2026 하반기 조사", updated.getName());
        assertEquals(STARTED.plusDays(1), updated.getStartedOn());
        assertEquals(STARTED.plusDays(30), updated.getEndedOn());
        assertNull(updated.getNote());
        assertEquals(project.getId(), updated.getId());
        assertEquals("2026 하반기 조사", service.getById(project.getId()).getName());
        assertEquals(List.of(10L), service.getTargetPointIds(project.getId()));
        assertEquals(1, service.getByProjectId(project.getId()).size());
    }

    @Test
    @DisplayName("수정 — 대상 재지정: 빠진 점은 대상·기록이 함께 지워지고, 남긴 점의 기록은 유지되고, 새 점은 대상에 든다")
    void update_reassignsTargets_deletesRemovedPointRecords() {
        SurveyProject project = sampleProject(10L, 11L);
        pointStore.add(12L);
        service.record(new RecordSurveyCommand(project.getId(), 10L, SurveyResult.INTACT, null, null));
        service.record(new RecordSurveyCommand(project.getId(), 11L, SurveyResult.LOST, null, null));

        service.update(new UpdateSurveyProjectCommand(
                project.getId(), "2026 일제조사", STARTED, null, null, List.of(11L, 12L)));

        assertEquals(List.of(11L, 12L), service.getTargetPointIds(project.getId()));
        List<SurveyRecordSummary> records = service.getByProjectId(project.getId());
        assertEquals(1, records.size()); // 10번 점의 기록은 대상에서 빠지며 함께 지워졌다
        assertEquals(11L, records.get(0).record().getPointId());
    }

    @Test
    @DisplayName("수정 — 대상이 비어 있거나 null 요소가 섞이면 거부한다(생성과 같은 규칙)")
    void update_withoutTargets_rejected() {
        SurveyProject project = sampleProject(10L);

        assertThrows(InvalidSurveyException.class, () -> service.update(new UpdateSurveyProjectCommand(
                project.getId(), "이름", STARTED, null, null, List.of())));
        assertThrows(InvalidSurveyException.class, () -> service.update(new UpdateSurveyProjectCommand(
                project.getId(), "이름", STARTED, null, null, null)));
        assertThrows(InvalidSurveyException.class, () -> service.update(new UpdateSurveyProjectCommand(
                project.getId(), "이름", STARTED, null, null, Arrays.asList(10L, null))));
        // 거부된 수정은 아무것도 남기지 않는다
        assertEquals("2026 일제조사", service.getById(project.getId()).getName());
        assertEquals(List.of(10L), service.getTargetPointIds(project.getId()));
    }

    @Test
    @DisplayName("수정 — 없는 점이 섞이면 전체를 거부하고 이름·대상·기록 모두 그대로다")
    void update_missingTargetPoint_rejectedWithoutChanges() {
        SurveyProject project = sampleProject(10L);
        service.record(new RecordSurveyCommand(project.getId(), 10L, SurveyResult.INTACT, null, null));

        assertThrows(ControlPointNotFoundException.class, () -> service.update(new UpdateSurveyProjectCommand(
                project.getId(), "바꾼 이름", STARTED, null, null, List.of(99L))));

        assertEquals("2026 일제조사", service.getById(project.getId()).getName());
        assertEquals(List.of(10L), service.getTargetPointIds(project.getId()));
        assertEquals(1, service.getByProjectId(project.getId()).size());
    }

    @Test
    @DisplayName("수정 — 없는 프로젝트는 거부하고, 종료일이 시작일보다 빠르면 도메인이 거부한다")
    void update_missingProjectOrReversedPeriod_throws() {
        SurveyProject project = sampleProject();

        assertThrows(SurveyProjectNotFoundException.class, () -> service.update(
                new UpdateSurveyProjectCommand(999L, "이름", STARTED, null, null, List.of(1L))));
        assertThrows(InvalidSurveyException.class, () -> service.update(
                new UpdateSurveyProjectCommand(project.getId(), "이름", STARTED, STARTED.minusDays(1), null, List.of(1L))));
    }

    @Test
    @DisplayName("삭제 — 프로젝트와 함께 대상 지정·조사 기록도 지운다")
    void delete_removesProjectWithTargetsAndRecords() {
        SurveyProject project = sampleProject(10L, 11L);
        service.record(new RecordSurveyCommand(project.getId(), 10L, SurveyResult.INTACT, null, null));

        service.delete(project.getId());

        assertTrue(service.getAll().isEmpty());
        assertTrue(targetStore.targets.isEmpty());
        assertTrue(store.findRecordsByProjectId(project.getId()).isEmpty());
    }

    @Test
    @DisplayName("없는 프로젝트 삭제는 SurveyProjectNotFoundException")
    void delete_missingProject_throws() {
        assertThrows(SurveyProjectNotFoundException.class, () -> service.delete(999L));
    }

    @Test
    @DisplayName("조사를 기록하면 조사 시각이 Clock(KST)으로 찍힌 레코드가 생긴다")
    void record_createsRecordWithClockTime() {
        SurveyProject project = sampleProject(10L);

        SurveyRecord record = service.record(
                new RecordSurveyCommand(project.getId(), 10L, SurveyResult.INTACT, "대상(2건)", null)).record();

        assertEquals(10L, record.getPointId());
        assertEquals(FIXED_KST, record.getSurveyedAt());
        assertEquals(SurveyResult.INTACT, record.getResult());
        assertEquals(1, service.getByProjectId(project.getId()).size());
    }

    @Test
    @DisplayName("같은 프로젝트×기준점에 다시 기록하면 새 레코드가 아니라 판정 정정이고, 조사원 이름이 실려 온다")
    void record_existing_revisesInsteadOfDuplicate() {
        SurveyProject project = sampleProject(10L);
        memberNames.names.put(7L, "김측량");
        SurveyRecord first = service.record(
                new RecordSurveyCommand(project.getId(), 10L, SurveyResult.INTACT, null, null)).record();

        SurveyRecordSummary revised = service.record(
                new RecordSurveyCommand(project.getId(), 10L, SurveyResult.LOST, "정정 비고", 7L));

        assertTrue(revised.record().isLost());
        assertEquals("정정 비고", revised.record().getNote()); // 정정 시 비고도 교체된다
        assertEquals("김측량", revised.surveyorName()); // 마지막 판정의 주체가 남는다
        assertEquals(1, service.getByProjectId(project.getId()).size()); // 새 레코드가 아니라 같은 행의 교체다
        assertEquals(SurveyResult.INTACT, first.getResult()); // 처음 판정은 그대로 두고 저장된 값만 바뀐다
    }

    @Test
    @DisplayName("없는 프로젝트·기준점에는 조사를 기록할 수 없다")
    void record_missingProjectOrPoint_throws() {
        SurveyProject project = sampleProject(10L);

        assertThrows(SurveyProjectNotFoundException.class,
                () -> service.record(new RecordSurveyCommand(99L, 10L, SurveyResult.INTACT, null, null)));
        assertThrows(ControlPointNotFoundException.class,
                () -> service.record(new RecordSurveyCommand(project.getId(), 99L, SurveyResult.INTACT, null, null)));
    }

    @Test
    @DisplayName("대상이 아닌 점에는 기록할 수 없다 — 기록은 대상으로 지정한 점에만 남는다")
    void record_nonTargetPoint_rejected() {
        SurveyProject project = sampleProject(10L);
        pointStore.add(11L); // 등록된 점이지만 이 프로젝트의 대상은 아니다

        assertThrows(SurveyTargetNotFoundException.class,
                () -> service.record(new RecordSurveyCommand(project.getId(), 11L, SurveyResult.INTACT, null, null)));
        assertTrue(service.getByProjectId(project.getId()).isEmpty());
    }

    @Test
    @DisplayName("조사 취소는 레코드를 삭제하고, 없는 기록 취소는 SurveyRecordNotFoundException")
    void cancel_deletesRecord() {
        SurveyProject project = sampleProject(10L);
        service.record(new RecordSurveyCommand(project.getId(), 10L, SurveyResult.INTACT, null, null));

        service.cancel(project.getId(), 10L);

        assertEquals(0, service.getByProjectId(project.getId()).size());
        assertThrows(SurveyRecordNotFoundException.class, () -> service.cancel(project.getId(), 10L));
    }

    @Test
    @DisplayName("없는 프로젝트의 조사기록 목록 조회는 SurveyProjectNotFoundException")
    void getRecords_missingProject_throws() {
        assertThrows(SurveyProjectNotFoundException.class, () -> service.getByProjectId(99L));
    }

    @Test
    @DisplayName("조사 현황 — 조사됨=기록 존재(망실 포함), 결과별 개수는 없는 결과도 0으로 채워 준다")
    void getProgress_countsRecordsByResult() {
        SurveyProject project = sampleProject(10L, 11L, 12L, 13L, 14L);
        service.record(new RecordSurveyCommand(project.getId(), 10L, SurveyResult.INTACT, null, null));
        service.record(new RecordSurveyCommand(project.getId(), 11L, SurveyResult.LOST, null, null));
        service.record(new RecordSurveyCommand(project.getId(), 12L, SurveyResult.INTACT, null, null));

        SurveyProgress progress = service.getProgress(project.getId());

        assertEquals("2026 일제조사", progress.projectName());
        assertEquals(5, progress.totalPoints());
        assertEquals(3, progress.surveyedPoints());
        assertEquals(2, progress.notSurveyedPoints());
        assertEquals(2, progress.countByResult().get(SurveyResult.INTACT));
        assertEquals(1, progress.countByResult().get(SurveyResult.LOST));
        assertEquals(0, progress.countByResult().get(SurveyResult.ETC));
        assertFalse(progress.complete()); // 3/5 조사 — 미완
    }

    @Test
    @DisplayName("조사 현황 — 기록이 하나도 없는 프로젝트는 조사됨 0, 결과별 개수도 모두 0으로 채운다")
    void getProgress_noRecords_fillsZeros() {
        SurveyProject project = sampleProject(10L, 11L);

        SurveyProgress progress = service.getProgress(project.getId());

        assertEquals(2, progress.totalPoints());
        assertEquals(0, progress.surveyedPoints());
        assertEquals(2, progress.notSurveyedPoints());
        assertEquals(0, progress.countByResult().get(SurveyResult.INTACT));
        assertEquals(0, progress.countByResult().get(SurveyResult.LOST));
        assertEquals(0, progress.countByResult().get(SurveyResult.ETC));
        assertFalse(progress.complete()); // 대상 2, 조사 0 — 미완
    }

    @Test
    @DisplayName("없는 프로젝트의 조사 현황 조회는 SurveyProjectNotFoundException")
    void getProgress_missingProject_throws() {
        assertThrows(SurveyProjectNotFoundException.class, () -> service.getProgress(99L));
    }

    @Test
    @DisplayName("진행률의 전체(total)는 전역 기준점 수가 아니라 프로젝트의 대상 점 수다")
    void getProgress_totalIsProjectTargetCount() {
        // 전역 기준점은 10개지만, 이 프로젝트의 조사 대상은 4개
        SurveyProject project = sampleProject(1L, 2L, 3L, 4L);
        Long pid = project.getId();
        for (long i = 5; i <= 10; i++) {
            pointStore.add(i);
        }
        service.record(new RecordSurveyCommand(pid, 1L, SurveyResult.INTACT, null, null));
        service.record(new RecordSurveyCommand(pid, 2L, SurveyResult.LOST, null, null));

        SurveyProgress progress = service.getProgress(pid);

        assertEquals(4, progress.totalPoints());       // 전역 10이 아니라 대상 4
        assertEquals(2, progress.surveyedPoints());
        assertEquals(2, progress.notSurveyedPoints()); // 4 - 2
        assertFalse(progress.complete()); // 2/4 조사 — 미완
    }

    @Test
    @DisplayName("대상이 전부 조사되면 complete=true")
    void getProgress_allTargetsSurveyed_complete() {
        SurveyProject project = sampleProject(1L, 2L);
        Long pid = project.getId();
        service.record(new RecordSurveyCommand(pid, 1L, SurveyResult.INTACT, null, null));
        service.record(new RecordSurveyCommand(pid, 2L, SurveyResult.LOST, null, null));

        SurveyProgress progress = service.getProgress(pid);

        assertTrue(progress.complete());
        assertEquals(0, progress.notSurveyedPoints());
    }

    @Test
    @DisplayName("대상이 아닌 점의 기록은 진행률에 안 들어간다 (오탐 완료 방지)")
    void getProgress_ignoresNonTargetRecords() {
        SurveyProject project = sampleProject(1L); // 대상은 1번만
        Long pid = project.getId();
        pointStore.add(2L);
        // 쓰기 경로는 비대상 기록을 거부하므로 저장소에 직접 심는다 — 집계 필터가 방어적 중복으로 남아 있는지 본다
        store.save(SurveyRecord.create(pid, 2L, SurveyResult.INTACT, FIXED_KST, null, null));

        SurveyProgress progress = service.getProgress(pid);

        assertEquals(1, progress.totalPoints());    // 대상 1
        assertEquals(0, progress.surveyedPoints());  // 비대상 기록은 미집계
        assertFalse(progress.complete());            // 대상이 미조사라 미완
    }

    /** 조사 포트 페이크 — 인메모리 저장으로 서비스 로직만 검증한다. 대상 포트는 FakeTargetStore 가 따로 맡는다. */
    private static class FakeSurveyStore implements LoadSurveyProjectPort, SaveSurveyProjectPort,
            DeleteSurveyProjectPort, LoadSurveyRecordPort, SaveSurveyRecordPort, DeleteSurveyRecordPort {

        // 실제 어댑터는 조인으로 이름을 함께 실어 온다 — 페이크는 회원 이름 페이크와 같은 표를 본다
        Map<Long, String> surveyorNames = Map.of();


        private final Map<Long, SurveyProject> projects = new HashMap<>();
        // 기록의 식별자는 (프로젝트, 기준점)이다 — 저장소도 같은 열쇠로 잡는다
        private final Map<List<Long>, SurveyRecord> records = new HashMap<>();
        // 진행률 집계가 대상 여부로 거른다 — 실제 쿼리의 exists 필터에 해당하는 참조
        private final FakeTargetStore targetStore;
        private long projectSeq = 0;

        FakeSurveyStore(FakeTargetStore targetStore) {
            this.targetStore = targetStore;
        }

        @Override
        public Optional<SurveyProject> findProjectById(Long id) {
            return Optional.ofNullable(projects.get(id));
        }

        @Override
        public List<SurveyProject> findAllProjects() {
            return new ArrayList<>(projects.values());
        }

        @Override
        public SurveyProject save(SurveyProject project) {
            long id = project.getId() != null ? project.getId() : ++projectSeq;
            SurveyProject saved = SurveyProject.restore(id, project.getAuthorId(), project.getName(), project.getStartedOn(), project.getEndedOn(), project.getNote());
            projects.put(id, saved);
            return saved;
        }

        @Override
        public List<SurveyRecord> findRecordsByProjectId(Long projectId) {
            return records.values().stream().filter(r -> r.getProjectId().equals(projectId)).toList();
        }

        @Override
        public List<SurveyRecord> findRecordsByPointId(Long pointId) {
            return records.values().stream().filter(r -> r.getPointId().equals(pointId)).toList();
        }

        /** 조사 시각이 겹치면 나중에 담은 기록이 이긴다 — 실제 어댑터가 기록을 만든 시각으로 가르는 것과 같은 규칙이다. */
        @Override
        public Optional<SurveyRecord> findLatestRecordByPointId(Long pointId) {
            return findRecordsByPointId(pointId).stream()
                    .reduce((older, newer) -> newer.getSurveyedAt().isBefore(older.getSurveyedAt()) ? older : newer);
        }

        @Override
        public List<PointLastSurvey> findLatestSurveyPerPoint() {
            throw new UnsupportedOperationException("조사 서비스는 점 전체의 최종조사를 읽지 않는다");
        }

        @Override
        public List<SurveyRecordSummary> findRecordSummariesByProjectId(Long projectId) {
            // 실제 어댑터는 조인으로 이름을 함께 실어 온다 — 페이크는 이름 없이 같은 목록을 돌려준다
            return findRecordsByProjectId(projectId).stream()
                    .map(record -> new SurveyRecordSummary(record, surveyorNames.get(record.getSurveyedById())))
                    .toList();
        }

        @Override
        public Optional<SurveyRecord> findRecordByProjectIdAndPointId(Long projectId, Long pointId) {
            return records.values().stream()
                    .filter(r -> r.getProjectId().equals(projectId) && r.getPointId().equals(pointId))
                    .findFirst();
        }

        @Override
        public boolean existsRecordByPointId(Long pointId) {
            return records.values().stream().anyMatch(r -> r.getPointId().equals(pointId));
        }

        @Override
        public Map<Long, Long> countSurveyedByProject() {
            // 실제 쿼리처럼 '대상'인 점의 기록만 센다
            Map<Long, Long> counts = new HashMap<>();
            records.values().stream()
                    .filter(r -> targetStore.targets.stream().anyMatch(
                            t -> t.getProjectId().equals(r.getProjectId()) && t.getPointId().equals(r.getPointId())))
                    .forEach(r -> counts.merge(r.getProjectId(), 1L, Long::sum));
            return counts;
        }

        @Override
        public Map<SurveyResult, Long> countByResult(Long projectId) {
            Set<Long> targetPoints = targetStore.targets.stream()
                    .filter(t -> t.getProjectId().equals(projectId))
                    .map(SurveyTarget::getPointId).collect(Collectors.toSet());
            Map<SurveyResult, Long> counts = new HashMap<>();
            records.values().stream()
                    .filter(r -> r.getProjectId().equals(projectId) && targetPoints.contains(r.getPointId()))
                    .forEach(r -> counts.merge(r.getResult(), 1L, Long::sum));
            return counts;
        }

        @Override
        public SurveyRecord save(SurveyRecord record) {
            records.put(List.of(record.getProjectId(), record.getPointId()), record);
            return record;
        }

        @Override
        public List<SurveyRecord> saveAll(List<SurveyRecord> list) {
            return list.stream().map(this::save).toList();
        }

        @Override
        public Optional<SurveyRecord> upsertForTarget(SurveyRecord record) {
            // 실제 문장과 같은 규칙: 대상 검사 → 있으면 전 필드 교체, 없으면 새 행
            boolean target = targetStore.targets.stream().anyMatch(
                    t -> t.getProjectId().equals(record.getProjectId()) && t.getPointId().equals(record.getPointId()));
            if (!target) {
                return Optional.empty();
            }
            records.put(List.of(record.getProjectId(), record.getPointId()), record);
            return Optional.of(record);
        }

        @Override
        public void deleteByProjectIdAndPointId(Long projectId, Long pointId) {
            records.values().removeIf(
                    r -> r.getProjectId().equals(projectId) && r.getPointId().equals(pointId));
        }

        @Override
        public void deleteByProjectId(Long projectId) {
            records.values().removeIf(r -> r.getProjectId().equals(projectId));
        }

        @Override
        public void deleteByProjectIdAndPointIds(Long projectId, List<Long> pointIds) {
            records.values().removeIf(
                    r -> r.getProjectId().equals(projectId) && pointIds.contains(r.getPointId()));
        }

        @Override
        public void deleteProjectById(Long id) {
            projects.remove(id);
        }
    }

    /** 조사 대상 포트 페이크 — 저장·삭제 포트가 각각 불리는지 검증할 수 있게 조사 포트와 분리한다. */
    private static class FakeTargetStore implements LoadSurveyTargetPort, SaveSurveyTargetPort, DeleteSurveyTargetPort {

        final List<SurveyTarget> targets = new ArrayList<>();

        @Override
        public long countByProjectId(Long projectId) {
            return targets.stream().filter(t -> t.getProjectId().equals(projectId)).count();
        }

        @Override
        public List<Long> findPointIdsByProjectId(Long projectId) {
            return targets.stream()
                    .filter(t -> t.getProjectId().equals(projectId))
                    .map(SurveyTarget::getPointId)
                    .toList();
        }

        @Override
        public boolean lockByProjectIdAndPointId(Long projectId, Long pointId) {
            // 잠금은 사진 업로드 축이 쓰는 경로다 — 조사 서비스 시험에서는 존재 여부만 같은 규칙으로 답한다
            return targets.stream()
                    .anyMatch(t -> t.getProjectId().equals(projectId) && t.getPointId().equals(pointId));
        }

        @Override
        public boolean existsByPointId(Long pointId) {
            return targets.stream().anyMatch(t -> t.getPointId().equals(pointId));
        }

        @Override
        public Map<Long, Long> countTargetsByProject() {
            Map<Long, Long> counts = new HashMap<>();
            targets.forEach(t -> counts.merge(t.getProjectId(), 1L, Long::sum));
            return counts;
        }

        @Override
        public SurveyTarget save(SurveyTarget target) {
            targets.add(target);
            return target;
        }

        @Override
        public List<SurveyTarget> saveAll(List<SurveyTarget> list) {
            targets.addAll(list);
            return List.copyOf(list);
        }

        @Override
        public void deleteByProjectId(Long projectId) {
            targets.removeIf(t -> t.getProjectId().equals(projectId));
        }

        @Override
        public void deleteByProjectIdAndPointIds(Long projectId, List<Long> pointIds) {
            targets.removeIf(t -> t.getProjectId().equals(projectId) && pointIds.contains(t.getPointId()));
        }
    }

    /** 작성자 이름 조회 페이크 — 등록된 회원만 이름을 돌려준다. */
    private static class FakeMemberNames implements LoadMemberNamesPort {

        final Map<Long, String> names = new HashMap<>();

        @Override
        public Map<Long, String> findNamesByIds(Collection<Long> ids) {
            Map<Long, String> found = new HashMap<>();
            ids.forEach(id -> {
                if (names.containsKey(id)) found.put(id, names.get(id));
            });
            return found;
        }
    }

    /** 기준점 존재 확인용 페이크. */
    private static class FakePointStore implements LoadControlPointPort, SaveControlPointPort {

        private final Map<Long, ControlPoint> points = new HashMap<>();

        @Override
        public ControlPoint save(ControlPoint point) {
            points.put(point.getId(), point);
            return point;
        }

        @Override
        public List<ControlPoint> saveAll(List<ControlPoint> batch) {
            batch.forEach(this::save);
            return batch;
        }

        void add(Long id) {
            points.put(id, ControlPoint.restore(
                    id, "41192D%012d".formatted(id), PointType.DOGEUN, "점" + id,
                    new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                            new BigDecimal("545000.00"), new BigDecimal("181000.00")),
                    new GeoCoordinate(126.79, 37.50),
                    null, null, null, null, null, null, null, null, null));
        }

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
            return points.values().stream().filter(p -> p.getName().equals(name) && p.getType() == type).findFirst();
        }

        @Override
        public List<ControlPoint> findAllByNameInOrPointNoIn(
                Collection<String> names, Collection<String> pointNos) {
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
        public List<PointLastSurvey> findSeedLastSurveys() {
            throw new UnsupportedOperationException("조사 서비스는 시드 최종조사를 읽지 않는다");
        }
    }
}
