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
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpoint.exception.DuplicateControlPointException;
import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 대상지 파일(CSV·XLSX) 임포트 — 한 파일로 조사 프로젝트 생성, 기준점 마스터 등록, 조사 대상 등록, 기존조사 이력 기록을 수행한다.
 * 이름·종류로 같은 물리적 점을 찾아 중복 등록을 막고(관리번호가 달라도), 기존 점의 성과가 다르면 CSV의 확정 성과로 갱신한다.
 *
 * 파일 파싱은 DB 를 건드리지 않으므로 트랜잭션 밖에서 끝낸다 — 수천 행 xlsx 를 읽는 동안 커넥션을 잡고 있으면
 * 동시에 다른 담당자가 올릴 때 풀이 마른다. 트랜잭션은 실제로 쓰는 구간(store)에만 건다.
 */
@Service
@RequiredArgsConstructor
public class SurveyCsvImportService implements ImportSurveyCsvUseCase {

    private final LoadControlPointPort loadControlPointPort;
    private final SaveControlPointPort saveControlPointPort;
    private final SaveSurveyProjectPort saveSurveyProjectPort;
    private final SaveSurveyRecordPort saveSurveyRecordPort;
    private final SaveSurveyTargetPort saveSurveyTargetPort;
    private final TableExtractor tableExtractor;
    private final CoordinateTransformer coordinateTransformer;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    /** 거부 메시지에 실을 오류 건수 상한. */
    private static final int ERRORS_IN_MESSAGE = 5;

    @Override
    public SurveyCsvImportResult importCsv(ImportSurveyCsvCommand command) {
        // 파싱·검증은 읽기만 하므로 트랜잭션 밖 — 여기서 거부되면 DB 는 시작조차 하지 않는다
        SurveyTargetMapper.MappingResult mapped = SurveyTargetMapper.map(tableExtractor.extract(command.content()));
        rejectIfAnyRowFailed(mapped.errors());

        return transactionTemplate.execute(status -> store(command, mapped.rows()));
    }

    /** 읽어 둔 행을 한 트랜잭션으로 저장한다 — 한 행이라도 실패하면 조사째로 되돌린다. */
    private SurveyCsvImportResult store(ImportSurveyCsvCommand command, List<Row> rows) {
        SurveyProject project = saveSurveyProjectPort.save(SurveyProject.create(
                command.name(), command.startedOn(), command.endedOn(), command.note()));

        // 행마다 찾으면 질의가 행 수만큼 늘어난다 — 파일에 나온 이름·관리번호를 한 번에 읽어 맞춘다
        List<ControlPoint> candidates = loadControlPointPort.findAllByNameInOrPointNoIn(
                rows.stream().map(Row::name).collect(Collectors.toSet()),
                rows.stream().map(Row::pointNo).collect(Collectors.toSet()));
        Map<String, ControlPoint> existing = candidates.stream()
                .collect(Collectors.toMap(p -> pointKey(p.getName(), p.getType()), p -> p, (first, second) -> first));
        Map<String, ControlPoint> byPointNo = candidates.stream()
                .collect(Collectors.toMap(ControlPoint::getPointNo, p -> p, (first, second) -> first));

        // 행과 기준점은 이름·종류로 잇는다 — 저장 순서에 기대면 어긋났을 때 대상이 엉뚱한 점에 붙는다
        Map<String, ControlPoint> resolved = new HashMap<>();
        List<ControlPoint> toRegister = new ArrayList<>();
        List<ControlPoint> toRevise = new ArrayList<>();

        for (Row row : rows) {
            TmCoordinate tm = new TmCoordinate(row.crs(), row.northing(), row.easting());
            // 경위도는 파일에 열이 있든 없든 성과에서 파생한다 — 권위값은 TM 이고, 파일의 경위도는 출처가 보증되지 않는다.
            // 덕분에 기본 양식과 경위도가 덧붙은 파일이 같은 결과를 낸다.
            GeoCoordinate geo = coordinateTransformer.toWgs84(tm);

            ControlPoint found = existing.get(pointKey(row.name(), row.type()));
            rejectIfPointNoTaken(row, found, byPointNo.get(row.pointNo()));
            if (found == null) {
                toRegister.add(register(row, tm, geo));
            } else if (unchanged(found, row, tm, geo)) {
                resolved.put(pointKey(row.name(), row.type()), found); // 성과·속성이 CSV와 동일 — 재사용
            } else {
                // 기존 점의 성과·속성이 CSV와 다르면(관리번호가 같아도) CSV의 확정값으로 갱신하고 id는 보존
                toRevise.add(revise(found.getId(), row, tm, geo));
            }
        }

        // 한 파일 안에서 이름·종류는 유일하므로(중복은 매퍼가 행 오류로 거른다) 저장 결과를 그 키로 되찾을 수 있다
        putByKey(resolved, saveControlPointPort.saveAll(toRegister));
        putByKey(resolved, saveControlPointPort.saveAll(toRevise));

        // 모든 행은 이 프로젝트의 조사 대상이다 — 진행률의 분모(전체 대상)가 된다.
        // 기본 양식에 없어 기준점 마스터로 옮기지 못한 열은 이 대상에 그대로 보관한다.
        List<SurveyTarget> targets = new ArrayList<>(rows.size());
        List<SurveyRecord> records = new ArrayList<>();
        for (Row row : rows) {
            Long pointId = resolved.get(pointKey(row.name(), row.type())).getId();
            targets.add(SurveyTarget.create(project.getId(), pointId, row.extras()));
            if (row.priorResult() != null) {
                records.add(SurveyRecord.create(
                        project.getId(), pointId, row.priorResult(), surveyedAt(row), row.note()));
            }
        }
        saveSurveyTargetPort.saveAll(targets);
        saveSurveyRecordPort.saveAll(records);

        return new SurveyCsvImportResult(
                project.getId(), rows.size(), toRegister.size(),
                rows.size() - toRegister.size() - toRevise.size(), toRevise.size(), records.size());
    }

    private static void putByKey(Map<String, ControlPoint> resolved, List<ControlPoint> saved) {
        saved.forEach(point -> resolved.put(pointKey(point.getName(), point.getType()), point));
    }

    /** 같은 물리적 점을 가리키는 키 — 관리번호는 출처마다 값이 달라 이름·종류로 맞춘다(부천 도근점은 이름 유일). */
    private static String pointKey(String name, PointType type) {
        return type + "|" + name;
    }

    /**
     * 파일의 관리번호를 이미 다른 점이 쓰고 있으면 멈춘다 — 관리번호는 유일해야 한다.
     * 그대로 두면 저장 제약에 걸려 원인을 알 수 없는 서버 오류가 되고, 어느 쪽 값이 맞는지는 사람이 판단할 일이다.
     */
    private static void rejectIfPointNoTaken(Row row, ControlPoint matched, ControlPoint owner) {
        if (owner == null || (matched != null && owner.getId().equals(matched.getId()))) {
            return;
        }
        throw new DuplicateControlPointException(
                "관리번호 " + row.pointNo() + "는 이미 다른 기준점(" + owner.getName() + ")이 쓰고 있습니다.");
    }

    /**
     * 한 행이라도 읽지 못하면 아무것도 등록하지 않는다 — 일부만 들어간 조사는 담당자가 무엇을 다시 올려야 할지 알 수 없다.
     * 오류가 많을 때 메시지가 끝없이 길어지지 않도록 앞쪽 몇 건만 싣는다(전체 목록은 미리보기가 보여 준다).
     */
    private void rejectIfAnyRowFailed(List<SurveyTargetMapper.RowError> errors) {
        if (errors.isEmpty()) {
            return;
        }
        String detail = errors.stream()
                .limit(ERRORS_IN_MESSAGE)
                .map(e -> e.row() + "행: " + e.message())
                .collect(Collectors.joining(" / "));
        if (errors.size() > ERRORS_IN_MESSAGE) {
            detail += " 외 " + (errors.size() - ERRORS_IN_MESSAGE) + "건";
        }
        throw new InvalidControlPointException(detail);
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
