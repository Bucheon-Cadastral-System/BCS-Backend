package com.is.bcs.adapter.out.persistence.controlpointimage;

import com.is.bcs.application.port.out.controlpoint.DeleteControlPointPort;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.application.port.out.controlpointimage.LoadControlPointImagePort;
import com.is.bcs.application.port.out.controlpointimage.SaveControlPointImagePort;
import com.is.bcs.application.port.out.survey.DeleteSurveyProjectPort;
import com.is.bcs.application.port.out.survey.SaveSurveyProjectPort;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpointimage.ControlPointImage;
import com.is.bcs.domain.survey.SurveyProject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 기준점 현장 사진 영속 왕복 검증(DB 필요, bcs/docker-compose). */
@SpringBootTest
@Transactional
class ControlPointImagePersistenceAdapterTest {

    private static final LocalDate STARTED = LocalDate.of(2026, 7, 1);
    private static final OffsetDateTime CAPTURED_AT = OffsetDateTime.parse("2026-07-01T10:00:00+09:00");

    @Autowired
    private SaveControlPointImagePort saveControlPointImagePort;

    @Autowired
    private LoadControlPointImagePort loadControlPointImagePort;

    @Autowired
    private SaveSurveyProjectPort saveSurveyProjectPort;

    @Autowired
    private DeleteSurveyProjectPort deleteSurveyProjectPort;

    @Autowired
    private SaveControlPointPort saveControlPointPort;

    @Autowired
    private DeleteControlPointPort deleteControlPointPort;

    @Autowired
    private EntityManager entityManager;

    private int projectSeq;
    private int pointSeq;
    private int imageSeq;

    private Long project() {
        projectSeq++;
        SurveyProject saved = saveSurveyProjectPort.save(
                SurveyProject.create(null, "현장 사진 시험 조사" + projectSeq, STARTED, null, null));
        entityManager.flush();
        return saved.getId();
    }

    /**
     * 기준점은 시퀀스로 채번되어 저장만으로는 행이 바로 반영되지 않을 수 있다.
     * 사진은 IDENTITY라 저장 즉시 반영되므로, 사진이 참조할 기준점 행을 미리 반영해 둔다.
     */
    private Long point() {
        pointSeq++;
        ControlPoint saved = saveControlPointPort.save(ControlPoint.register(
                "41192D%09d".formatted(pointSeq), PointType.DOGEUN, "시험점" + pointSeq,
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                        new BigDecimal("545236.77"), new BigDecimal("181840.96")),
                new GeoCoordinate(126.794623, 37.506423),
                null, null, null, null, null, null, null, null, null));
        entityManager.flush();
        return saved.getId();
    }

    private ControlPointImage image(Long projectId, Long pointId) {
        imageSeq++;
        return saveControlPointImagePort.save(ControlPointImage.create(
                projectId, pointId,
                "시험사진%d.webp".formatted(imageSeq),
                "control-points/%d/projects/%d/시험사진%d.webp".formatted(pointId, projectId, imageSeq),
                "IMG_%04d.webp".formatted(imageSeq),
                "image/webp", 204_800L, 1920, 1080, CAPTURED_AT, null));
    }

    @Test
    @DisplayName("사진을 저장한 뒤 id로 조회하면 파일 정보와 촬영 시각이 보존된다")
    void saveAndFindById_preservesAttributes() {
        Long projectId = project();
        Long pointId = point();

        ControlPointImage saved = saveControlPointImagePort.save(ControlPointImage.create(
                projectId, pointId,
                "1465공_11111111-1111-1111-1111-111111111111.webp",
                "control-points/%d/projects/%d/1465공.webp".formatted(pointId, projectId),
                "IMG_0001.webp", "image/webp", 204_800L, 1920, 1080, CAPTURED_AT, null));

        ControlPointImage found = loadControlPointImagePort.findById(saved.getId()).orElseThrow();

        assertEquals(projectId, found.getProjectId());
        assertEquals(pointId, found.getPointId());
        assertEquals("1465공_11111111-1111-1111-1111-111111111111.webp", found.getStoredFileName());
        assertEquals("IMG_0001.webp", found.getOriginalFileName());
        assertEquals("image/webp", found.getContentType());
        assertEquals(204_800L, found.getFileSize());
        assertEquals(1920, found.getWidth());
        assertEquals(1080, found.getHeight());
        // timestamptz는 instant를 보존한다(offset은 정규화될 수 있어 같은 순간인지로 비교한다)
        assertTrue(found.getCapturedAt().isEqual(CAPTURED_AT));
        assertNotNull(found.getCreatedAt());
    }

    @Test
    @DisplayName("프로젝트×기준점으로 조회하면 그 조합의 사진만 돌아온다")
    void findByProjectIdAndPointId_returnsMatchingImageOnly() {
        Long projectId = project();
        Long pointId = point();
        Long otherPointId = point();
        ControlPointImage saved = image(projectId, pointId);

        ControlPointImage found = loadControlPointImagePort.findByProjectIdAndPointId(projectId, pointId).orElseThrow();

        assertEquals(saved.getId(), found.getId());
        assertTrue(loadControlPointImagePort.existsByProjectIdAndPointId(projectId, pointId));
        assertFalse(loadControlPointImagePort.existsByProjectIdAndPointId(projectId, otherPointId));
    }

    @Test
    @DisplayName("프로젝트별 조회는 다른 프로젝트의 사진을 섞지 않는다")
    void findAllByProjectId_isolatesOtherProjects() {
        Long projectId = project();
        Long otherProjectId = project();
        Long pointId = point();
        Long otherPointId = point();
        ControlPointImage mine1 = image(projectId, pointId);
        ControlPointImage mine2 = image(projectId, otherPointId);
        image(otherProjectId, pointId);

        Page<ControlPointImage> found = loadControlPointImagePort.findAllByProjectId(projectId, PageRequest.of(0, 20));

        assertEquals(2, found.getContent().size());
        assertEquals(Set.of(mine1.getId(), mine2.getId()),
                found.getContent().stream().map(ControlPointImage::getId).collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("기준점별 조회는 다른 점의 사진을 섞지 않는다")
    void findAllByPointId_isolatesOtherPoints() {
        Long pointId = point();
        Long otherPointId = point();
        Long projectId = project();
        Long otherProjectId = project();
        ControlPointImage mine1 = image(projectId, pointId);
        ControlPointImage mine2 = image(otherProjectId, pointId);
        image(projectId, otherPointId);

        Page<ControlPointImage> found = loadControlPointImagePort.findAllByPointId(pointId, PageRequest.of(0, 20));

        assertEquals(2, found.getContent().size());
        assertEquals(Set.of(mine1.getId(), mine2.getId()),
                found.getContent().stream().map(ControlPointImage::getId).collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("프로젝트를 삭제하면 그 프로젝트의 사진 행도 함께 삭제된다")
    void deleteProject_cascadesToItsImages() {
        Long projectId = project();
        Long otherProjectId = project();
        Long pointId = point();
        ControlPointImage removed = image(projectId, pointId);
        ControlPointImage kept = image(otherProjectId, point());

        // 포트는 사진 행을 직접 지우지 않는다. 여기서 확인하는 것은 외래키의 ON DELETE CASCADE다.
        deleteSurveyProjectPort.deleteProjectById(projectId);
        entityManager.flush();
        entityManager.clear(); // 1차 캐시가 아니라 DB에서 다시 확인한다

        assertTrue(loadControlPointImagePort.findById(removed.getId()).isEmpty());
        assertTrue(loadControlPointImagePort.findById(kept.getId()).isPresent());
    }

    @Test
    @DisplayName("기준점을 삭제하면 그 점의 사진 행도 함께 삭제된다")
    void deleteControlPoint_cascadesToItsImages() {
        Long pointId = point();
        Long otherPointId = point();
        Long projectId = project();
        ControlPointImage removed = image(projectId, pointId);
        ControlPointImage kept = image(project(), otherPointId);

        // 포트는 사진 행을 직접 지우지 않는다. 여기서 확인하는 것은 외래키의 ON DELETE CASCADE다.
        deleteControlPointPort.deleteById(pointId);
        entityManager.flush();
        entityManager.clear(); // 1차 캐시가 아니라 DB에서 다시 확인한다

        assertTrue(loadControlPointImagePort.findById(removed.getId()).isEmpty());
        assertTrue(loadControlPointImagePort.findById(kept.getId()).isPresent());
    }
}
