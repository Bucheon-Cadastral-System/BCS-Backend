package com.is.bcs.application.service;

import com.is.bcs.application.dto.SurveyProjectExportFile;
import com.is.bcs.application.dto.SurveyRecordSummary;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.file.Table;
import com.is.bcs.application.port.out.survey.LoadSurveyProjectPort;
import com.is.bcs.application.port.out.survey.LoadSurveyRecordPort;
import com.is.bcs.application.port.out.survey.LoadSurveyTargetPort;
import com.is.bcs.config.TimeConfig;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyResult;
import com.is.bcs.domain.survey.exception.SurveyProjectNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 내보내기 표의 열 차례·어휘·최종조사 선택 규칙. 파일 형식은 XlsxTableWriterTest 가 따로 본다. */
class SurveyProjectExportServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), TimeConfig.KST);

    private final LoadSurveyProjectPort projects = mock(LoadSurveyProjectPort.class);
    private final LoadSurveyTargetPort targets = mock(LoadSurveyTargetPort.class);
    private final LoadSurveyRecordPort records = mock(LoadSurveyRecordPort.class);
    private final LoadControlPointPort points = mock(LoadControlPointPort.class);
    private final AtomicReference<Table> written = new AtomicReference<>();

    private final SurveyProjectExportService service = new SurveyProjectExportService(
            projects, targets, records, points,
            (sheetName, table) -> {
                written.set(table);
                return new byte[]{1};
            },
            CLOCK);

    @Test
    @DisplayName("열 차례와 어휘 — 대상지 파일이 요구하는 열 뒤에 최종조사 네 열")
    void columns() {
        given(point(1L, "1465공", PointType.DOGEUN, "완전", LocalDate.parse("2025-09-08")));
        when(records.findRecordSummariesByProjectId(7L)).thenReturn(List.of(
                summary(record(7L, 1L, SurveyResult.INTACT, "2026-08-01T00:00:00+09:00", null), null)));
        when(records.findLatestRecordSummariesByPointIds(any())).thenReturn(List.of(
                summary(record(7L, 1L, SurveyResult.LOST, "2026-08-01T00:00:00+09:00", "표석 확인 불가"), "김민석")));

        service.export(7L);

        assertEquals(List.of(
                "종류", "기준점명", "기준점번호", "좌표계구분", "X좌표", "Y좌표", "경도(X)", "위도(Y)",
                "토지소재지", "상세주소", "설치일자", "기존조사내용", "기존조사일",
                "최종조사내용", "최종조사일자", "최종조사원", "비고"), written.get().headers());
        assertEquals(List.of(
                "지적도근점", "1465공", "41192D000001265", "세계", "545236.77", "181840.96",
                "126.794623", "37.506423", "10300-춘의동", "경기도 부천시 춘의동 102-16", "2018-02-21",
                "정상", "2026-08-01",
                "망실", "2026-08-01", "김민석", "표석 확인 불가"), written.get().rows().getFirst());
    }

    @Test
    @DisplayName("조사하지 않은 대상 — 기존조사 두 칸이 빈다")
    void notSurveyed() {
        given(point(1L, "1465공", PointType.DOGEUN, null, null));
        when(records.findRecordSummariesByProjectId(7L)).thenReturn(List.of());
        when(records.findLatestRecordSummariesByPointIds(any())).thenReturn(List.of());

        service.export(7L);

        List<String> row = written.get().rows().getFirst();
        assertEquals("", row.get(11));
        assertEquals("", row.get(12));
        assertEquals("", row.get(13));
    }

    @Test
    @DisplayName("최종조사 — 시드가 기록보다 늦으면 시드를 세우고 조사원은 비운다")
    void seedWinsWhenNewer() {
        given(point(1L, "1465공", PointType.DOGEUN, "완전", LocalDate.parse("2026-09-08")));
        when(records.findRecordSummariesByProjectId(7L)).thenReturn(List.of());
        when(records.findLatestRecordSummariesByPointIds(any())).thenReturn(List.of(
                summary(record(7L, 1L, SurveyResult.LOST, "2026-08-01T00:00:00+09:00", "비고"), "김민석")));

        service.export(7L);

        List<String> row = written.get().rows().getFirst();
        assertEquals("완전", row.get(13));
        assertEquals("2026-09-08", row.get(14));
        assertEquals("", row.get(15));
        assertEquals("", row.get(16));
    }

    @Test
    @DisplayName("차례 — 종류 순, 이름의 숫자는 값 순")
    void ordering() {
        when(projects.findProjectById(7L)).thenReturn(Optional.of(project()));
        when(targets.findPointIdsByProjectId(7L)).thenReturn(List.of(1L, 2L, 3L));
        when(points.findAllByIds(any())).thenReturn(List.of(
                point(1L, "10공", PointType.DOGEUN, null, null),
                point(2L, "2공", PointType.DOGEUN, null, null),
                point(3L, "가1", PointType.TRIANGULATION_AUX, null, null)));
        when(records.findRecordSummariesByProjectId(7L)).thenReturn(List.of());
        when(records.findLatestRecordSummariesByPointIds(any())).thenReturn(List.of());

        service.export(7L);

        assertEquals(List.of("가1", "2공", "10공"), written.get().rows().stream().map(row -> row.get(1)).toList());
    }

    @Test
    @DisplayName("저장 이름 — 조사명에 파일 이름으로 쓸 수 없는 글자가 있어도 저장된다")
    void fileName() {
        when(projects.findProjectById(7L)).thenReturn(Optional.of(SurveyProject.restore(
                7L, null, "2026/8 굴착", LocalDate.parse("2026-08-01"), null, null)));
        when(targets.findPointIdsByProjectId(7L)).thenReturn(List.of());
        when(points.findAllByIds(any())).thenReturn(List.of());
        when(records.findRecordSummariesByProjectId(7L)).thenReturn(List.of());
        when(records.findLatestRecordSummariesByPointIds(any())).thenReturn(List.of());

        SurveyProjectExportFile file = service.export(7L);

        assertEquals("2026 8 굴착_기준점.xlsx", file.fileName());
    }

    @Test
    @DisplayName("없는 조사를 내보내려 하면 거절한다")
    void unknownProject() {
        when(projects.findProjectById(7L)).thenReturn(Optional.empty());
        assertThrows(SurveyProjectNotFoundException.class, () -> service.export(7L));
    }

    private void given(ControlPoint point) {
        when(projects.findProjectById(7L)).thenReturn(Optional.of(project()));
        when(targets.findPointIdsByProjectId(7L)).thenReturn(List.of(point.getId()));
        when(points.findAllByIds(any())).thenReturn(List.of(point));
    }

    private static SurveyProject project() {
        return SurveyProject.restore(7L, null, "2026년 8월 조사", LocalDate.parse("2026-08-01"), null, null);
    }

    private static ControlPoint point(Long id, String name, PointType type, String lastResult, LocalDate lastOn) {
        return ControlPoint.restore(
                id, "41192D000001265", type, name,
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL, new BigDecimal("545236.77"), new BigDecimal("181840.96")),
                new GeoCoordinate(126.794623, 37.506423),
                "10300", "춘의동", "경기도 부천시 춘의동 102-16",
                MarkerMaterial.STONE, InstallType.INSTALLED, LocalDate.parse("2018-02-21"), null,
                lastResult, lastOn);
    }

    private static SurveyRecord record(Long projectId, Long pointId, SurveyResult result, String at, String note) {
        return SurveyRecord.restore(projectId, pointId, result, OffsetDateTime.parse(at), note, null);
    }

    private static SurveyRecordSummary summary(SurveyRecord record, String surveyorName) {
        return new SurveyRecordSummary(record, surveyorName);
    }
}
