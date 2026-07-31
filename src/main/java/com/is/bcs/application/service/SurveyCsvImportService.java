package com.is.bcs.application.service;

import com.is.bcs.application.dto.SurveyCsvImportResult;
import com.is.bcs.application.dto.ImportSurveyCsvCommand;
import com.is.bcs.application.port.in.imports.ImportSurveyCsvUseCase;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.application.port.out.survey.SaveSurveyProjectPort;
import com.is.bcs.application.port.out.survey.SaveSurveyRecordPort;
import com.is.bcs.application.port.out.file.TableExtractor;
import com.is.bcs.application.port.out.geo.CoordinateTransformer;
import com.is.bcs.application.port.out.survey.SaveSurveyTargetPort;
import com.is.bcs.application.service.SurveyTargetMapper.Row;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 대상지 파일(CSV·XLSX) 임포트 — 한 파일로 조사 프로젝트 생성, 기준점 마스터 등록, 조사 대상 등록, 기존조사 이력 기록을 수행한다.
 * 프로젝트 유형은 요청이 정한다 — 파일 서식과 조사 계기는 별개 축이라, 같은 서식을 어떤 계기로든 임포트할 수 있어야 한다.
 * 이름·종류로 같은 물리적 점을 찾아 중복 등록을 막고(관리번호가 달라도), 기존 점의 성과가 다르면 CSV의 확정 성과로 갱신한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SurveyCsvImportService implements ImportSurveyCsvUseCase {

    private final LoadControlPointPort loadControlPointPort;
    private final SaveControlPointPort saveControlPointPort;
    private final SaveSurveyProjectPort saveSurveyProjectPort;
    private final SaveSurveyRecordPort saveSurveyRecordPort;
    private final SaveSurveyTargetPort saveSurveyTargetPort;
    private final TableExtractor tableExtractor;
    private final CoordinateTransformer coordinateTransformer;
    private final Clock clock;

    @Override
    public SurveyCsvImportResult importCsv(ImportSurveyCsvCommand command) {
        List<Row> rows = SurveyTargetMapper.map(tableExtractor.extract(command.content()));

        SurveyProject project = saveSurveyProjectPort.save(SurveyProject.create(
                command.type(), command.name(), command.note()));

        int newPoints = 0;
        int existingPoints = 0;
        int updatedPoints = 0;
        int createdRecords = 0;

        for (Row row : rows) {
            TmCoordinate tm = new TmCoordinate(row.crs(), row.northing(), row.easting());
            // 경위도는 파일에 열이 있든 없든 성과에서 파생한다 — 권위값은 TM 이고, 파일의 경위도는 출처가 보증되지 않는다.
            // 덕분에 기본 양식과 경위도가 덧붙은 파일이 같은 결과를 낸다.
            GeoCoordinate geo = coordinateTransformer.toWgs84(tm);

            ControlPoint existing = loadControlPointPort.findByNameAndType(row.name(), row.type()).orElse(null);
            ControlPoint point;
            if (existing == null) {
                point = saveControlPointPort.save(register(row, tm, geo));
                newPoints++;
            } else if (unchanged(existing, row, tm, geo)) {
                point = existing; // 성과·속성이 CSV와 동일 — 재사용
                existingPoints++;
            } else {
                // 기존 점의 성과·속성이 CSV와 다르면(관리번호가 같아도) CSV의 확정값으로 갱신하고 id는 보존
                point = saveControlPointPort.save(revise(existing.getId(), row, tm, geo));
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

        return new SurveyCsvImportResult(
                project.getId(), rows.size(), newPoints, existingPoints, updatedPoints, createdRecords);
    }

    private ControlPoint register(Row row, TmCoordinate tm, GeoCoordinate geo) {
        return ControlPoint.register(
                row.pointNo(), row.type(), row.name(), tm, geo,
                row.regionCode(), row.regionName(), row.address(),
                row.markerMaterial(), row.installType(), row.installedDate(),
                row.traverse());
    }

    /**
     * 기존 점의 성과·속성이 CSV 행과 완전히 같은지 — 같으면 갱신이 필요 없다(재사용).
     * 관리번호가 같아도 좌표·주소·설치정보가 바뀌었으면 false. BigDecimal은 자릿수 차이를 무시하려 compareTo로 본다.
     */
    private boolean unchanged(ControlPoint p, Row row, TmCoordinate tm, GeoCoordinate geo) {
        return p.getPointNo().equals(row.pointNo())
                && p.getTm().crs() == tm.crs()
                && p.getTm().northing().compareTo(tm.northing()) == 0
                && p.getTm().easting().compareTo(tm.easting()) == 0
                && p.getGeo().equals(geo)
                && Objects.equals(p.getRegionCode(), row.regionCode())
                && Objects.equals(p.getRegionName(), row.regionName())
                && Objects.equals(p.getAddress(), row.address())
                && p.getMarkerMaterial() == row.markerMaterial()
                && p.getInstallType() == row.installType()
                && Objects.equals(p.getInstalledDate(), row.installedDate())
                && Objects.equals(p.getTraverse(), row.traverse());
    }

    /** 기존 점을 CSV 성과로 갱신 — id는 보존하고 관리번호·좌표·속성을 최신 값으로 덮는다. */
    private ControlPoint revise(Long id, Row row, TmCoordinate tm, GeoCoordinate geo) {
        return ControlPoint.restore(
                id, row.pointNo(), row.type(), row.name(), tm, geo,
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
