package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyProjectType;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 조사 프로젝트·조사기록 영속 왕복 검증 — DB 필요(bcs/docker-compose). */
@SpringBootTest
@Transactional
class SurveyPersistenceAdapterTest {

    private static final OffsetDateTime SURVEYED_AT = OffsetDateTime.parse("2025-09-08T10:00:00+09:00");

    @Autowired
    private SurveyPersistenceAdapter adapter;

    @Autowired
    private SurveyRecordJpaRepository recordRepository;

    private SurveyProject savedProject() {
        return adapter.save(SurveyProject.create(
                SurveyProjectType.EXCAVATION_CONSULTATION, "2026 굴착협의", "협의번호 2333"));
    }

    @Test
    @DisplayName("프로젝트 저장 후 조회하면 유형·이름·비고가 보존된다")
    void saveAndFindProject_preservesAttributes() {
        SurveyProject saved = savedProject();

        SurveyProject found = adapter.findProjectById(saved.getId()).orElseThrow();

        assertNotNull(found.getId());
        assertEquals(SurveyProjectType.EXCAVATION_CONSULTATION, found.getType());
        assertEquals("2026 굴착협의", found.getName());
        assertEquals("협의번호 2333", found.getNote());
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
}
