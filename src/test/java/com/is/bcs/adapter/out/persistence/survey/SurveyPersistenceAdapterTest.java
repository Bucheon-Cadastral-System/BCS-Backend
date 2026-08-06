package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.application.port.out.member.SaveMemberPort;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpoint.TraverseInfo;
import com.is.bcs.domain.member.Member;
import com.is.bcs.domain.survey.ExtraColumn;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import com.is.bcs.domain.survey.SurveyTarget;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 조사 프로젝트·조사기록 영속 왕복 검증 — DB 필요(bcs/docker-compose). */
@SpringBootTest
@Transactional
class SurveyPersistenceAdapterTest {

    private static final LocalDate STARTED = LocalDate.of(2026, 7, 1);

    private static final OffsetDateTime SURVEYED_AT = OffsetDateTime.parse("2025-09-08T10:00:00+09:00");

    @Autowired
    private SurveyPersistenceAdapter adapter;

    @Autowired
    private SurveyRecordJpaRepository recordRepository;

    @Autowired
    private SurveyTargetPersistenceAdapter targetAdapter;

    @Autowired
    private SurveyTargetJpaRepository targetRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SaveControlPointPort pointPort;

    @Autowired
    private SaveMemberPort memberPort;

    private int pointSeq = 0;

    // 회원은 기준점과 따로 센다 — 순번을 나눠 쓰면 점 없이 회원만 두 번 만들 때 같은 카카오 id 가 겹친다
    private int memberSeq = 0;

    private SurveyProject savedProject() {
        return adapter.save(SurveyProject.create(null, "2026 일제조사", STARTED, null, "정기 조사"));
    }

    /**
     * 조사 대상·기록이 가리키는 기준점은 실재해야 한다 — 대상이 기준점을, 기록이 대상을 참조하는
     * 외래키가 걸려 있어 임의의 id 로는 저장 자체가 되지 않는다(그게 이 제약이 하는 일이다).
     */
    private long savedPointId() {
        pointSeq++;
        return pointPort.save(ControlPoint.register(
                "41192D%09d".formatted(pointSeq), PointType.DOGEUN, "시험점" + pointSeq,
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                        new BigDecimal("545236.77"), new BigDecimal("181840.96")),
                new GeoCoordinate(126.794623, 37.506423),
                "10300", "춘의동", "경기도 부천시 춘의동 102-16",
                MarkerMaterial.STEEL, InstallType.INSTALLED, LocalDate.of(2018, 2, 21),
                new TraverseInfo("1", null, null, false), null, null, null)).getId();
    }

    /** 대상으로 지정된 점 — 기록은 대상에만 남길 수 있으므로 기록 시험은 이 자리를 먼저 만든다. */
    private long targetPointId(SurveyProject project) {
        long pointId = savedPointId();
        targetAdapter.save(SurveyTarget.create(project.getId(), pointId));
        return pointId;
    }

    private long savedMemberId() {
        memberSeq++;
        return memberPort.save(Member.registerWithKakao("kakao-" + memberSeq, SURVEYED_AT)).getId();
    }

    @Test
    @DisplayName("프로젝트 저장 후 조회하면 기간·이름·비고가 보존된다")
    void saveAndFindProject_preservesAttributes() {
        SurveyProject saved = savedProject();

        SurveyProject found = adapter.findProjectById(saved.getId()).orElseThrow();

        assertNotNull(found.getId());
        assertEquals(STARTED, found.getStartedOn());
        assertEquals("2026 일제조사", found.getName());
        assertEquals("정기 조사", found.getNote());
        assertEquals(1, adapter.findAllProjects().size());
    }

    @Test
    @DisplayName("조사기록 저장 후 조회하면 결과가 보존되고 조사 시각은 같은 순간이다")
    void saveAndFindRecord_preservesAttributes() {
        SurveyProject project = savedProject();
        long pointId = targetPointId(project);
        long memberId = savedMemberId();

        adapter.save(SurveyRecord.create(project.getId(), pointId, SurveyResult.LOST, SURVEYED_AT, "대상(2건)", memberId));

        SurveyRecord found = adapter.findRecordByProjectIdAndPointId(project.getId(), pointId).orElseThrow();
        assertEquals(SurveyResult.LOST, found.getResult());
        assertEquals("대상(2건)", found.getNote());
        assertEquals(memberId, found.getSurveyedById());
        // timestamptz는 instant 보존(offset은 정규화될 수 있음) — 같은 순간인지로 비교
        assertTrue(found.getSurveyedAt().isEqual(SURVEYED_AT));
        assertEquals(1, adapter.findRecordsByProjectId(project.getId()).size());
    }

    @Test
    @DisplayName("조사기록을 프로젝트×기준점으로 삭제한다")
    void deleteByProjectIdAndPointId_removesRecord() {
        SurveyProject project = savedProject();
        long pointId = targetPointId(project);
        adapter.save(SurveyRecord.create(project.getId(), pointId, SurveyResult.INTACT, SURVEYED_AT, null, null));

        adapter.deleteByProjectIdAndPointId(project.getId(), pointId);

        assertTrue(adapter.findRecordByProjectIdAndPointId(project.getId(), pointId).isEmpty());
    }

    @Test
    @DisplayName("같은 프로젝트×기준점의 조사기록은 두 번 만들 수 없다")
    void duplicateProjectPoint_rejected() {
        SurveyProject project = savedProject();
        long pointId = targetPointId(project);
        adapter.save(SurveyRecord.create(project.getId(), pointId, SurveyResult.INTACT, SURVEYED_AT, null, null));

        assertThrows(DataIntegrityViolationException.class, () -> {
            adapter.save(SurveyRecord.create(project.getId(), pointId, SurveyResult.LOST, SURVEYED_AT, null, null));
            recordRepository.flush(); // 유니크 제약은 flush 시점에 검증된다
        });
    }

    @Test
    @DisplayName("결과별 개수는 해당 프로젝트의 대상 점 기록만 결과별로 센다")
    void countByResult_groupsOwnProjectRecords() {
        SurveyProject project = savedProject();
        SurveyProject other = adapter.save(SurveyProject.create(null, "다른 조사", STARTED, null, null));
        long first = targetPointId(project);
        long second = targetPointId(project);
        long third = targetPointId(project);
        // 다른 조사도 같은 점을 대상으로 삼는다 — 회차가 달라도 대상은 회차별로 따로 선다
        targetAdapter.save(SurveyTarget.create(other.getId(), first));
        adapter.save(SurveyRecord.create(project.getId(), first, SurveyResult.INTACT, SURVEYED_AT, null, null));
        adapter.save(SurveyRecord.create(project.getId(), second, SurveyResult.LOST, SURVEYED_AT, null, null));
        adapter.save(SurveyRecord.create(project.getId(), third, SurveyResult.INTACT, SURVEYED_AT, null, null));
        adapter.save(SurveyRecord.create(other.getId(), first, SurveyResult.ETC, SURVEYED_AT, null, null));

        Map<SurveyResult, Long> counts = adapter.countByResult(project.getId());

        assertEquals(2, counts.get(SurveyResult.INTACT)); // 다른 조사의 기록은 섞이지 않는다
        assertEquals(1, counts.get(SurveyResult.LOST));
        assertEquals(2, counts.size()); // 기록 없는 결과(기타)는 키가 없다 — 0 채움은 서비스 몫
    }

    @Test
    @DisplayName("대상이 아닌 점의 기록은 저장 자체가 거부된다 — 진행률의 전제를 DB 가 지킨다")
    void recordWithoutTarget_rejected() {
        SurveyProject project = savedProject();
        long pointId = savedPointId(); // 대상으로 지정하지 않은 점

        assertThrows(DataIntegrityViolationException.class, () -> {
            adapter.save(SurveyRecord.create(project.getId(), pointId, SurveyResult.INTACT, SURVEYED_AT, null, null));
            recordRepository.flush();
        });
    }

    @Test
    @DisplayName("대상에서 빠지면 그 점의 기록도 함께 사라진다 — 재지정이 남긴 기록을 DB 가 거둔다")
    void deletingTarget_cascadesRecord() {
        SurveyProject project = savedProject();
        long pointId = targetPointId(project);
        adapter.save(SurveyRecord.create(project.getId(), pointId, SurveyResult.INTACT, SURVEYED_AT, null, null));
        recordRepository.flush();

        targetAdapter.deleteByProjectIdAndPointIds(project.getId(), List.of(pointId));
        entityManager.flush();
        entityManager.clear();

        assertTrue(adapter.findRecordByProjectIdAndPointId(project.getId(), pointId).isEmpty());
    }

    @Test
    @DisplayName("원자 upsert — 대상이면 쓰고, 다시 부르면 같은 행을 정정하며, 대상이 아니면 아무것도 쓰지 않는다")
    void upsertForTarget_writesRevisesAndRefuses() {
        SurveyProject project = savedProject();
        long pointId = targetPointId(project);
        long memberId = savedMemberId();

        SurveyRecord written = adapter.upsertForTarget(SurveyRecord.create(
                project.getId(), pointId, SurveyResult.INTACT, SURVEYED_AT, "최초", null)).orElseThrow();
        assertEquals(SurveyResult.INTACT, written.getResult());

        OffsetDateTime revisedAt = SURVEYED_AT.plusDays(1);
        SurveyRecord revised = adapter.upsertForTarget(SurveyRecord.create(
                project.getId(), pointId, SurveyResult.LOST, revisedAt, null, memberId)).orElseThrow();
        assertEquals(written.getId(), revised.getId()); // 정정은 새 행이 아니라 같은 행이다
        assertEquals(SurveyResult.LOST, revised.getResult());
        assertTrue(revised.getSurveyedAt().isEqual(revisedAt)); // 조사 시각도 정정한 시각으로 바뀐다
        assertEquals(memberId, revised.getSurveyedById()); // 마지막 판정의 주체가 남는다
        assertEquals(null, revised.getNote()); // 전 필드 교체 — 비고 없는 정정은 비고를 지운다
        assertEquals(1, adapter.findRecordsByProjectId(project.getId()).size());

        long outsider = savedPointId();
        assertTrue(adapter.upsertForTarget(SurveyRecord.create(
                project.getId(), outsider, SurveyResult.INTACT, SURVEYED_AT, null, null)).isEmpty());
        assertEquals(1, adapter.findRecordsByProjectId(project.getId()).size());
    }

    @Test
    @DisplayName("정정은 최초 기록 시각을 지킨다 — 갱신이지 재생성이 아니다")
    void upsertForTarget_keepsCreatedAt() {
        SurveyProject project = savedProject();
        long pointId = targetPointId(project);

        adapter.upsertForTarget(SurveyRecord.create(
                project.getId(), pointId, SurveyResult.INTACT, SURVEYED_AT, null, null));
        OffsetDateTime createdAt = auditOf(project.getId(), pointId).getCreatedAt();

        adapter.upsertForTarget(SurveyRecord.create(
                project.getId(), pointId, SurveyResult.LOST, SURVEYED_AT.plusDays(1), null, null));

        SurveyRecordJpaEntity revised = auditOf(project.getId(), pointId);
        // 도메인 객체는 감사 시각을 들고 다니지 않으므로 저장된 행을 직접 읽는다
        assertTrue(createdAt.isEqual(revised.getCreatedAt()));
        assertTrue(!revised.getUpdatedAt().isBefore(revised.getCreatedAt()));
    }

    /** 감사 시각까지 보려면 도메인이 아니라 저장된 행을 읽어야 한다 — 1차 캐시가 아니라 DB 에서. */
    private SurveyRecordJpaEntity auditOf(Long projectId, Long pointId) {
        entityManager.clear();
        return recordRepository.findByProjectIdAndPointId(projectId, pointId).orElseThrow();
    }

    @Test
    @DisplayName("조사 대상에 보관한 열은 이름·값·순서가 그대로 돌아온다")
    void saveAndFindTarget_preservesExtraColumns() {
        SurveyProject project = savedProject();
        SurveyTarget saved = targetAdapter.save(SurveyTarget.create(project.getId(), savedPointId(), List.of(
                new ExtraColumn("순번", "131"),
                new ExtraColumn("점검자", "김주무관"),
                new ExtraColumn("field_20", null))));

        targetRepository.flush();
        entityManager.clear(); // 1차 캐시가 아니라 DB에서 다시 읽는다

        SurveyTarget found = targetRepository.findById(saved.getId()).orElseThrow().toDomain();
        assertEquals(List.of(
                new ExtraColumn("순번", "131"),
                new ExtraColumn("점검자", "김주무관"),
                new ExtraColumn("field_20", null)), found.getExtras());
    }

    @Test
    @DisplayName("보관 열 값은 길이 제한 없이 들어간다 — 뜻을 모르는 열이라 길이를 예상할 수 없다")
    void saveTarget_acceptsLongExtraValue() {
        SurveyProject project = savedProject();
        String longValue = "가".repeat(5000);

        SurveyTarget saved = targetAdapter.save(
                SurveyTarget.create(project.getId(), savedPointId(), List.of(new ExtraColumn("현장 메모", longValue))));
        targetRepository.flush();
        entityManager.clear();

        SurveyTarget found = targetRepository.findById(saved.getId()).orElseThrow().toDomain();
        assertEquals(longValue, found.getExtras().getFirst().value());
    }
}
