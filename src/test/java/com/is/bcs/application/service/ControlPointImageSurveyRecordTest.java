package com.is.bcs.application.service;

import com.is.bcs.application.dto.StoredControlPointImageFile;
import com.is.bcs.application.dto.UploadControlPointImageCommand;
import com.is.bcs.application.port.in.controlpointimage.UploadControlPointImageUseCase;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.application.port.out.controlpointimage.ControlPointImageFileStoragePort;
import com.is.bcs.application.port.out.controlpointimage.LoadControlPointImagePort;
import com.is.bcs.application.port.out.member.SaveMemberPort;
import com.is.bcs.application.port.out.survey.LoadSurveyRecordPort;
import com.is.bcs.application.port.out.survey.SaveSurveyProjectPort;
import com.is.bcs.application.port.out.survey.SaveSurveyTargetPort;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpoint.TraverseInfo;
import com.is.bcs.domain.controlpointimage.exception.InvalidControlPointImageException;
import com.is.bcs.domain.member.District;
import com.is.bcs.domain.member.Member;
import com.is.bcs.domain.member.Position;
import com.is.bcs.domain.member.Team;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import com.is.bcs.domain.survey.SurveyTarget;
import com.is.bcs.domain.survey.exception.SurveyTargetNotFoundException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 현장 사진과 판정이 한 트랜잭션인지 검증한다 — DB 필요(bcs/docker-compose).
 *
 * <p>파일 저장은 대역으로 세운다. 여기서 보려는 것은 사진 바이트가 아니라 사진 행과 조사기록이
 * 함께 남고 함께 사라지는가다. 저장 자체의 왕복은 {@code LocalControlPointImageStorageAdapterTest} 가 본다.
 */
@SpringBootTest
@Transactional
class ControlPointImageSurveyRecordTest {

    private static final OffsetDateTime CAPTURED_AT = OffsetDateTime.parse("2026-08-01T10:30:00+09:00");

    @Autowired
    private UploadControlPointImageUseCase useCase;

    @Autowired
    private LoadControlPointImagePort loadImagePort;

    @Autowired
    private LoadSurveyRecordPort loadRecordPort;

    @Autowired
    private SaveControlPointPort savePointPort;

    @Autowired
    private SaveSurveyProjectPort saveProjectPort;

    @Autowired
    private SaveSurveyTargetPort saveTargetPort;

    @Autowired
    private SaveMemberPort saveMemberPort;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private ControlPointImageFileStoragePort fileStoragePort;

    private int seq = 0;

    @BeforeEach
    void stubStorage() {
        when(fileStoragePort.store(anyLong(), anyLong(), anyString(), any(), any(), any(), anyLong(), any()))
                .thenAnswer(invocation -> new StoredControlPointImageFile(
                        "1465공_20260801.webp",
                        "control-points/1/projects/1/1465공_20260801.webp",
                        "field.webp", "image/webp", 1024L, 800, 600));
    }

    private long savedPointId() {
        seq++;
        return savePointPort.save(ControlPoint.register(
                "41192D%09d".formatted(seq), PointType.DOGEUN, "시험점" + seq,
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                        new BigDecimal("545236.77"), new BigDecimal("181840.96")),
                new GeoCoordinate(126.794623, 37.506423),
                "10300", "춘의동", "경기도 부천시 춘의동 102-16",
                MarkerMaterial.STEEL, InstallType.INSTALLED, LocalDate.of(2018, 2, 21),
                new TraverseInfo("1", null, null, false), null, null)).getId();
    }

    /** 활성 회원만 사진을 올릴 수 있으므로 프로필을 채우고 승인까지 마친 회원을 만든다. */
    private long savedMemberId() {
        seq++;
        Member member = Member.registerWithKakao("kakao-img-" + seq, CAPTURED_AT);
        member.completeProfile("조사원" + seq, "01012345678", "img" + seq + "@example.com",
                District.WONMI, "민원지적과", Team.CADASTRAL_MANAGEMENT, Position.OFFICER);
        member.approve(CAPTURED_AT);
        return saveMemberPort.save(member).getId();
    }

    private UploadControlPointImageCommand command(
            long projectId, long pointId, long uploaderId,
            OffsetDateTime capturedAt, SurveyResult result, String note
    ) {
        return new UploadControlPointImageCommand(
                projectId, pointId, "field.webp", "image/webp", 1024L, new byte[]{1, 2, 3},
                capturedAt, result, note, uploaderId);
    }

    @Test
    @DisplayName("사진을 올리면 같은 요청에서 판정이 함께 남고, 조사 시각은 올린 시각이 아니라 촬영 시각이다")
    void upload_recordsSurveyWithCapturedAt() {
        long pointId = savedPointId();
        long uploaderId = savedMemberId();
        SurveyProject project = saveProjectPort.save(
                SurveyProject.create(null, "2026 일제조사", LocalDate.of(2026, 7, 1), null, null));
        saveTargetPort.save(SurveyTarget.create(project.getId(), pointId));

        useCase.uploadOrReplace(command(project.getId(), pointId, uploaderId, CAPTURED_AT, SurveyResult.LOST, null));
        entityManager.flush();
        entityManager.clear();

        SurveyRecord record = loadRecordPort
                .findRecordByProjectIdAndPointId(project.getId(), pointId).orElseThrow();
        // 사진만 보고 서버가 정상으로 단정하지 않는다 — 올린 사람이 고른 판정이 그대로 남는다
        assertEquals(SurveyResult.LOST, record.getResult());
        assertEquals(uploaderId, record.getSurveyedById());
        assertEquals(CAPTURED_AT.toInstant(), record.getSurveyedAt().toInstant());
        assertTrue(loadImagePort.findByProjectIdAndPointId(project.getId(), pointId).isPresent());
    }

    @Test
    @DisplayName("기타로 올리면 사유가 기록에 함께 남는다")
    void upload_keepsNoteForEtc() {
        long pointId = savedPointId();
        long uploaderId = savedMemberId();
        SurveyProject project = saveProjectPort.save(
                SurveyProject.create(null, "2026 일제조사", LocalDate.of(2026, 7, 1), null, null));
        saveTargetPort.save(SurveyTarget.create(project.getId(), pointId));

        useCase.uploadOrReplace(
                command(project.getId(), pointId, uploaderId, CAPTURED_AT, SurveyResult.ETC, "표석 일부 파손"));
        entityManager.flush();
        entityManager.clear();

        SurveyRecord record = loadRecordPort
                .findRecordByProjectIdAndPointId(project.getId(), pointId).orElseThrow();
        assertEquals(SurveyResult.ETC, record.getResult());
        assertEquals("표석 일부 파손", record.getNote());
    }

    @Test
    @DisplayName("대상이 아닌 점이면 사진도 판정도 남지 않는다")
    void upload_rejectsNonTarget() {
        long pointId = savedPointId();
        long uploaderId = savedMemberId();
        long otherPointId = savedPointId();
        SurveyProject project = saveProjectPort.save(
                SurveyProject.create(null, "2026 일제조사", LocalDate.of(2026, 7, 1), null, null));
        saveTargetPort.save(SurveyTarget.create(project.getId(), otherPointId));

        assertThrows(SurveyTargetNotFoundException.class, () -> useCase.uploadOrReplace(
                command(project.getId(), pointId, uploaderId, CAPTURED_AT, SurveyResult.INTACT, null)));

        assertTrue(loadImagePort.findByProjectIdAndPointId(project.getId(), pointId).isEmpty());
        assertTrue(loadRecordPort.findRecordByProjectIdAndPointId(project.getId(), pointId).isEmpty());
    }

    @Test
    @DisplayName("촬영 일시가 미래면 거부한다 — 그 기록이 최종조사 자리를 영구히 차지한다")
    void upload_rejectsFutureCapturedAt() {
        long pointId = savedPointId();
        long uploaderId = savedMemberId();
        SurveyProject project = saveProjectPort.save(
                SurveyProject.create(null, "2026 일제조사", LocalDate.of(2026, 7, 1), null, null));
        saveTargetPort.save(SurveyTarget.create(project.getId(), pointId));

        assertThrows(InvalidControlPointImageException.class, () -> useCase.uploadOrReplace(
                command(project.getId(), pointId, uploaderId,
                        OffsetDateTime.now().plusDays(1), SurveyResult.INTACT, null)));

        assertTrue(loadRecordPort.findRecordByProjectIdAndPointId(project.getId(), pointId).isEmpty());
    }

    @Test
    @DisplayName("사진을 바꿔 올리면 판정도 새 값으로 정정된다 — 기록이 두 벌이 되지 않는다")
    void replace_correctsSurveyRecord() {
        long pointId = savedPointId();
        long uploaderId = savedMemberId();
        SurveyProject project = saveProjectPort.save(
                SurveyProject.create(null, "2026 일제조사", LocalDate.of(2026, 7, 1), null, null));
        saveTargetPort.save(SurveyTarget.create(project.getId(), pointId));

        useCase.uploadOrReplace(
                command(project.getId(), pointId, uploaderId, CAPTURED_AT, SurveyResult.INTACT, null));
        entityManager.flush();

        OffsetDateTime later = CAPTURED_AT.plusDays(3);
        useCase.uploadOrReplace(
                command(project.getId(), pointId, uploaderId, later, SurveyResult.UNAVAILABLE, null));
        entityManager.flush();
        entityManager.clear();

        SurveyRecord record = loadRecordPort
                .findRecordByProjectIdAndPointId(project.getId(), pointId).orElseThrow();
        assertEquals(SurveyResult.UNAVAILABLE, record.getResult());
        assertEquals(later.toInstant(), record.getSurveyedAt().toInstant());
        assertNull(record.getNote());
        assertEquals(1, loadRecordPort.findRecordsByProjectId(project.getId()).size());
    }
}
