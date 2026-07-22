package com.is.bcs.application.service;

import com.is.bcs.application.dto.ExcavationImportResult;
import com.is.bcs.application.dto.ImportExcavationCsvCommand;
import com.is.bcs.application.port.in.imports.ImportExcavationCsvUseCase;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.application.port.out.survey.SaveSurveyProjectPort;
import com.is.bcs.application.port.out.survey.SaveSurveyRecordPort;
import com.is.bcs.application.service.ExcavationCsvParser.Row;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyProjectType;
import com.is.bcs.domain.survey.SurveyRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 굴착협의 CSV 임포트 — 한 파일로 굴착협의 프로젝트 생성, 기준점 마스터 등록, 기존조사 이력 기록을 수행한다.
 * 이미 등록된 관리번호는 마스터를 다시 만들지 않고 재사용한다(성과 갱신은 임포트 책임이 아님).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ExcavationCsvImportService implements ImportExcavationCsvUseCase {

    private final LoadControlPointPort loadControlPointPort;
    private final SaveControlPointPort saveControlPointPort;
    private final SaveSurveyProjectPort saveSurveyProjectPort;
    private final SaveSurveyRecordPort saveSurveyRecordPort;
    private final Clock clock;

    @Override
    public ExcavationImportResult importCsv(ImportExcavationCsvCommand command) {
        List<Row> rows = ExcavationCsvParser.parse(command.content());

        SurveyProject project = saveSurveyProjectPort.save(SurveyProject.create(
                SurveyProjectType.EXCAVATION_CONSULTATION, command.name(), command.note()));

        int newPoints = 0;
        int existingPoints = 0;
        int createdRecords = 0;

        for (Row row : rows) {
            ControlPoint point = loadControlPointPort.findByPointNo(row.pointNo()).orElse(null);
            if (point == null) {
                point = saveControlPointPort.save(register(row));
                newPoints++;
            } else {
                existingPoints++;
            }

            if (row.priorResult() != null) {
                saveSurveyRecordPort.save(SurveyRecord.create(
                        project.getId(), point.getId(), row.priorResult(), surveyedAt(row), row.note()));
                createdRecords++;
            }
        }

        return new ExcavationImportResult(project.getId(), rows.size(), newPoints, existingPoints, createdRecords);
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

    /** 조사 시각 = 기존조사일의 KST 자정. 조사일이 비어 있으면 임포트 시각으로 방어한다. */
    private OffsetDateTime surveyedAt(Row row) {
        if (row.priorSurveyDate() != null) {
            return row.priorSurveyDate().atStartOfDay(clock.getZone()).toOffsetDateTime();
        }
        return OffsetDateTime.now(clock);
    }
}
