package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.domain.survey.ExtraColumn;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import com.is.bcs.domain.survey.SurveyTarget;
import jakarta.persistence.EntityManager;
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

    private SurveyProject savedProject() {
        return adapter.save(SurveyProject.create("2026 일제조사", STARTED, null, "정기 조사"));
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

        adapter.save(SurveyRecord.create(project.getId(), 10L, SurveyResult.LOST, SURVEYED_AT, "대상(2건)"));

        SurveyRecord found = adapter.findRecordByProjectIdAndPointId(project.getId(), 10L).orElseThrow();
        assertEquals(SurveyResult.LOST, found.getResult());
        assertEquals("대상(2건)", found.getNote());
        // timestamptz는 instant 보존(offset은 정규화될 수 있음) — 같은 순간인지로 비교
        assertTrue(found.getSurveyedAt().isEqual(SURVEYED_AT));
        assertEquals(1, adapter.findRecordsByProjectId(project.getId()).size());
    }

    @Test
    @DisplayName("조사기록을 프로젝트×기준점으로 삭제한다")
    void deleteByProjectIdAndPointId_removesRecord() {
        SurveyProject project = savedProject();
        adapter.save(SurveyRecord.create(project.getId(), 10L, SurveyResult.INTACT, SURVEYED_AT, null));

        adapter.deleteByProjectIdAndPointId(project.getId(), 10L);

        assertTrue(adapter.findRecordByProjectIdAndPointId(project.getId(), 10L).isEmpty());
    }

    @Test
    @DisplayName("같은 프로젝트×기준점의 조사기록은 두 번 만들 수 없다")
    void duplicateProjectPoint_rejected() {
        SurveyProject project = savedProject();
        adapter.save(SurveyRecord.create(project.getId(), 10L, SurveyResult.INTACT, SURVEYED_AT, null));

        assertThrows(DataIntegrityViolationException.class, () -> {
            adapter.save(SurveyRecord.create(project.getId(), 10L, SurveyResult.LOST, SURVEYED_AT, null));
            recordRepository.flush(); // 유니크 제약은 flush 시점에 검증된다
        });
    }

    @Test
    @DisplayName("결과별 개수는 해당 프로젝트의 대상 점 기록만 결과별로 센다")
    void countByResult_groupsOwnProjectRecords() {
        SurveyProject project = savedProject();
        SurveyProject other = adapter.save(SurveyProject.create("다른 조사", STARTED, null, null));
        for (long pointId : new long[]{10L, 11L, 12L}) {
            targetAdapter.save(SurveyTarget.create(project.getId(), pointId));
        }
        adapter.save(SurveyRecord.create(project.getId(), 10L, SurveyResult.INTACT, SURVEYED_AT, null));
        adapter.save(SurveyRecord.create(project.getId(), 11L, SurveyResult.LOST, SURVEYED_AT, null));
        adapter.save(SurveyRecord.create(project.getId(), 12L, SurveyResult.INTACT, SURVEYED_AT, null));
        adapter.save(SurveyRecord.create(other.getId(), 10L, SurveyResult.ETC, SURVEYED_AT, null));
        // 대상으로 지정되지 않은 점의 기록 — 진행률이 오탐되지 않도록 집계에서 빠져야 한다
        adapter.save(SurveyRecord.create(project.getId(), 13L, SurveyResult.INTACT, SURVEYED_AT, null));

        Map<SurveyResult, Long> counts = adapter.countByResult(project.getId());

        assertEquals(2, counts.get(SurveyResult.INTACT)); // 13번은 비대상이라 제외
        assertEquals(1, counts.get(SurveyResult.LOST));
        assertEquals(2, counts.size()); // 기록 없는 결과(기타)는 키가 없다 — 0 채움은 서비스 몫
    }

    @Test
    @DisplayName("조사 대상에 보관한 열은 이름·값·순서가 그대로 돌아온다")
    void saveAndFindTarget_preservesExtraColumns() {
        SurveyProject project = savedProject();
        SurveyTarget saved = targetAdapter.save(SurveyTarget.create(project.getId(), 10L, List.of(
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
}
