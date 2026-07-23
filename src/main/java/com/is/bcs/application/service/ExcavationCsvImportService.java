package com.is.bcs.application.service;

import com.is.bcs.application.dto.ExcavationImportResult;
import com.is.bcs.application.dto.ImportExcavationCsvCommand;
import com.is.bcs.application.port.in.imports.ImportExcavationCsvUseCase;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.application.port.out.survey.SaveSurveyProjectPort;
import com.is.bcs.application.port.out.survey.SaveSurveyRecordPort;
import com.is.bcs.application.port.out.survey.SaveSurveyTargetPort;
import com.is.bcs.application.service.ExcavationCsvParser.Row;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyProjectType;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 굴착협의 CSV 임포트 — 한 파일로 굴착협의 프로젝트 생성, 기준점 마스터 등록, 조사 대상 등록, 기존조사 이력 기록을 수행한다.
 * 이름·종류로 같은 물리적 점을 찾아 중복 등록을 막고(관리번호가 달라도), 기존 점의 성과가 다르면 CSV의 확정 성과로 갱신한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ExcavationCsvImportService implements ImportExcavationCsvUseCase {

    private final LoadControlPointPort loadControlPointPort;
    private final SaveControlPointPort saveControlPointPort;
    private final SaveSurveyProjectPort saveSurveyProjectPort;
    private final SaveSurveyRecordPort saveSurveyRecordPort;
    private final SaveSurveyTargetPort saveSurveyTargetPort;
    private final Clock clock;

    @Override
    public ExcavationImportResult importCsv(ImportExcavationCsvCommand command) {
        List<Row> rows = ExcavationCsvParser.parse(command.content());

        SurveyProject project = saveSurveyProjectPort.save(SurveyProject.create(
                SurveyProjectType.EXCAVATION_CONSULTATION, command.name(), command.note()));

        int newPoints = 0;
        int existingPoints = 0;
        int updatedPoints = 0;
        int createdRecords = 0;

        for (Row row : rows) {
            ControlPoint existing = loadControlPointPort.findByNameAndType(row.name(), row.type()).orElse(null);
            ControlPoint point;
            if (existing == null) {
                point = saveControlPointPort.save(register(row));
                newPoints++;
            } else if (existing.getPointNo().equals(row.pointNo())) {
                point = existing; // 이미 CSV 성과 — 재사용(갱신 없음)
                existingPoints++;
            } else {
                // 관리번호·성과가 다른 기존 점(시드 등 placeholder) → CSV의 확정 성과로 갱신하고 id는 보존
                point = saveControlPointPort.save(revise(existing.getId(), row));
                updatedPoints++;
            }

            // 모든 행은 이 프로젝트의 조사 대상이다 — 진행률의 분모(전체 대상)가 된다
            saveSurveyTargetPort.save(SurveyTarget.create(project.getId(), point.getId()));

            if (row.priorResult() != null) {
                saveSurveyRecordPort.save(SurveyRecord.create(
                        project.getId(), point.getId(), row.priorResult(), surveyedAt(row), row.note()));
                createdRecords++;
            }
        }

        return new ExcavationImportResult(
                project.getId(), rows.size(), newPoints, existingPoints, updatedPoints, createdRecords);
    }

    private ControlPoint register(Row row) {
        return ControlPoint.register(
                row.pointNo(), row.type(), row.name(),
                new TmCoordinate(row.crs(), row.northing(), row.easting()),
                new GeoCoordinate(row.longitude(), row.latitude()),
                row.regionCode(), row.regionName(), row.address(),
                row.markerMaterial(), row.installType(), row.installedDate(),
                row.traverse());
    }

    /** 기존 점을 CSV 성과로 갱신 — id는 보존하고 관리번호·좌표·속성을 최신 값으로 덮는다. */
    private ControlPoint revise(Long id, Row row) {
        return ControlPoint.restore(
                id, row.pointNo(), row.type(), row.name(),
                new TmCoordinate(row.crs(), row.northing(), row.easting()),
                new GeoCoordinate(row.longitude(), row.latitude()),
                row.regionCode(), row.regionName(), row.address(),
                row.markerMaterial(), row.installType(), row.installedDate(),
                row.traverse());
    }

    /** 조사 시각 = 기존조사일의 KST 자정. 조사일이 비어 있으면 임포트 시각으로 방어한다. */
    private OffsetDateTime surveyedAt(Row row) {
        if (row.priorSurveyDate() != null) {
            return row.priorSurveyDate().atStartOfDay(clock.getZone()).toOffsetDateTime();
        }
        return OffsetDateTime.now(clock);
    }
}
