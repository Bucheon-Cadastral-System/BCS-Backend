package com.is.bcs.application.service;

import com.is.bcs.application.dto.FieldChange;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.application.service.ImportFileMapper.Row;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TraverseInfo;
import com.is.bcs.domain.controlpoint.exception.DuplicateControlPointException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 파일에서 읽은 행을 기준점 마스터에 반영한다 — 없으면 등록하고, 있는데 성과가 다르면 파일 값으로 갱신한다.
 * 기준점만 올리는 경로와 조사 대상지를 올리는 경로가 같은 규칙을 써야 하므로 여기에 모아 둔다.
 */
@Component
@RequiredArgsConstructor
public class ControlPointRegistrar {

    private final LoadControlPointPort loadControlPointPort;
    private final SaveControlPointPort saveControlPointPort;

    /**
     * 반영 결과 — 행에 해당하는 기준점을 되찾을 수 있는 표와 건수.
     * 조사 대상지 경로가 대상·기록을 붙이려면 행마다 어느 기준점인지 알아야 한다.
     */
    public record Result(Map<String, ControlPoint> byKey, int newPoints, int updatedPoints, int existingPoints) {

        public ControlPoint pointOf(Row row) {
            ControlPoint point = byKey.get(pointKey(row.name(), row.type()));
            if (point == null) {
                // 파일 내 중복은 매퍼가 걸러 여기 올 수 없다 — 온다면 내부 불변식이 깨진 것이라 행을 밝혀 남긴다
                throw new IllegalStateException(
                        row.sourceRow() + "행의 기준점을 저장 결과에서 찾지 못했습니다: " + row.name());
            }
            return point;
        }
    }

    /**
     * 파일에 나온 이름·관리번호로 기존 점을 한 번에 읽는다 — 행마다 찾으면 질의가 파일 크기만큼 늘어난다.
     * 이름·종류(같은 점 찾기)와 관리번호(충돌 확인) 두 방향에서 맞춰 봐야 해서 표가 둘이다.
     */
    public record Candidates(Map<String, ControlPoint> byKey, Map<String, ControlPoint> byPointNo) {

        /** 이 행이 어느 기존 점에 붙는지 — 없으면 신규다. */
        public ControlPoint match(Row row) {
            return byKey.get(pointKey(row.name(), row.type()));
        }

        /** 이 행의 관리번호를 이미 쥔 점 — 붙는 점과 다르면 충돌이다. */
        public ControlPoint pointNoOwner(Row row) {
            return byPointNo.get(row.pointNo());
        }
    }

    public Candidates candidates(List<Row> rows) {
        List<ControlPoint> found = loadControlPointPort.findAllByNameInOrPointNoIn(
                rows.stream().map(Row::name).collect(Collectors.toSet()),
                rows.stream().map(Row::pointNo).collect(Collectors.toSet()));
        return new Candidates(
                found.stream().collect(Collectors.toMap(
                        p -> pointKey(p.getName(), p.getType()), p -> p, (first, second) -> first)),
                found.stream().collect(Collectors.toMap(
                        ControlPoint::getPointNo, p -> p, (first, second) -> first)));
    }

    public Result register(List<Row> rows) {
        Candidates candidates = candidates(rows);
        Map<String, ControlPoint> existing = candidates.byKey();

        // 행과 기준점은 이름·종류로 잇는다 — 저장 순서에 기대면 어긋났을 때 대상이 엉뚱한 점에 붙는다
        Map<String, ControlPoint> resolved = new HashMap<>();
        List<ControlPoint> toRegister = new ArrayList<>();
        List<ControlPoint> toRevise = new ArrayList<>();

        for (Row row : rows) {
            ControlPoint found = existing.get(pointKey(row.name(), row.type()));
            rejectIfPointNoTaken(row, found, candidates.pointNoOwner(row));
            if (found == null) {
                toRegister.add(toPoint(null, row));
            } else if (changes(found, row).isEmpty()) {
                resolved.put(pointKey(row.name(), row.type()), found); // 성과·속성이 파일과 동일 — 재사용
            } else {
                // 기존 점의 성과·속성이 파일과 다르면(관리번호가 같아도) 파일의 확정값으로 갱신하고 id는 보존
                toRevise.add(toPoint(found, row));
            }
        }

        // 한 파일 안에서 이름·종류는 유일하므로(중복은 매퍼가 행 오류로 거른다) 저장 결과를 그 키로 되찾을 수 있다
        putByKey(resolved, saveControlPointPort.saveAll(toRegister));
        putByKey(resolved, saveControlPointPort.saveAll(toRevise));

        return new Result(resolved, toRegister.size(), toRevise.size(),
                rows.size() - toRegister.size() - toRevise.size());
    }

    private static void putByKey(Map<String, ControlPoint> resolved, List<ControlPoint> saved) {
        saved.forEach(point -> resolved.put(pointKey(point.getName(), point.getType()), point));
    }

    /** 같은 물리적 점을 가리키는 키 — 관리번호는 출처마다 값이 달라 이름·종류로 맞춘다(부천 도근점은 이름 유일). */
    private static String pointKey(String name, PointType type) {
        return type + "|" + name;
    }

    /**
     * 파일의 관리번호를 다른 점이 쓰고 있으면 그 사유를, 아니면 null.
     * 등록은 이걸로 멈추고(어느 쪽 값이 맞는지는 사람이 판단할 일), 미리보기는 행 오류로 미리 보여 준다 —
     * 등록에서만 검사하면 미리보기는 통과인데 등록이 실패하는 불일치가 남는다.
     */
    public static String pointNoTakenBy(Row row, ControlPoint matched, ControlPoint owner) {
        if (owner == null || (matched != null && owner.getId().equals(matched.getId()))) {
            return null;
        }
        return "관리번호 " + row.pointNo() + "가 다른 기준점(" + owner.getName() + ")에 등록되어 있습니다.";
    }

    private static void rejectIfPointNoTaken(Row row, ControlPoint matched, ControlPoint owner) {
        String taken = pointNoTakenBy(row, matched, owner);
        if (taken != null) {
            throw new DuplicateControlPointException(taken);
        }
    }

    /**
     * 기존 점이 있으면 그 점을 파일 값으로 되살리고(갱신, id 보존), 없으면 새 점으로 만든다.
     * 선택 항목은 값이 있을 때만 반영한다 — 열 없는 파일(최소 6열)과 입력 칸 없는 수동 등록이
     * 기존 값을 지우지 않게. 빈 칸은 '모른다'이고, 지우기는 화면의 몫이다.
     * 시드 조사(최종조사내용·일)는 두 칸이 한 쌍이라 따로 고르지 않는다. 날짜가 늦은 쪽을 통째로 남긴다.
     */
    private static ControlPoint toPoint(ControlPoint found, Row row) {
        return found == null
                ? ControlPoint.register(
                        row.pointNo(), row.type(), row.name(), row.tm(), row.geo(),
                        row.regionCode(), row.regionName(), row.address(),
                        row.markerMaterial(), row.installType(), row.installedDate(), row.traverse(),
                        row.lastResult(), row.lastSurveyDate())
                : ControlPoint.restore(
                        found.getId(), row.pointNo(), row.type(), row.name(), row.tm(), row.geo(),
                        orKept(row.regionCode(), found, ControlPoint::getRegionCode),
                        orKept(row.regionName(), found, ControlPoint::getRegionName),
                        orKept(row.address(), found, ControlPoint::getAddress),
                        orKept(row.markerMaterial(), found, ControlPoint::getMarkerMaterial),
                        orKept(row.installType(), found, ControlPoint::getInstallType),
                        orKept(row.installedDate(), found, ControlPoint::getInstalledDate),
                        orKept(row.traverse(), found, ControlPoint::getTraverse),
                        seedSurvey(found, row).result(),
                        seedSurvey(found, row).surveyedOn());
    }

    /** 기준점이 든 시드 조사 한 쌍. */
    private record SeedSurvey(String result, LocalDate surveyedOn) {
    }

    /**
     * 시드 조사를 고른다 — 조사일이 늦은 쪽이 이긴다.
     *
     * <p>시드는 이 시스템에 올라오기 전까지의 총정리라 새 파일이 늘 최신인 것이 아니다.
     * 옛 자료로 만든 파일을 나중에 올리면 최신 시드가 옛 값으로 덮인다.
     *
     * <p>두 칸을 함께 정한다. 칸마다 따로 고르면 내용은 새 파일, 날짜는 옛 파일 같은 섞인 값이 남는다.
     * 새 파일의 조사일을 알 수 없으면 늦다는 것을 증명할 수 없으므로 기존 값을 지킨다.
     */
    private static SeedSurvey seedSurvey(ControlPoint found, Row row) {
        SeedSurvey kept = new SeedSurvey(found.getLastSurveyResult(), found.getLastSurveyedOn());
        if (row.lastResult() == null && row.lastSurveyDate() == null) {
            return kept; // 열 없는 파일 — 손대지 않는다
        }
        if (kept.surveyedOn() == null) {
            return new SeedSurvey(row.lastResult(), row.lastSurveyDate());
        }
        if (row.lastSurveyDate() == null || row.lastSurveyDate().isBefore(kept.surveyedOn())) {
            return kept;
        }
        return new SeedSurvey(row.lastResult(), row.lastSurveyDate());
    }

    private static <T> T orKept(T fromRow, ControlPoint found, Function<ControlPoint, T> kept) {
        return fromRow != null ? fromRow : kept.apply(found);
    }

    /**
     * 기존 점이 파일 행과 어디가 다른지 — 비어 있으면 갱신이 필요 없다(재사용).
     * 관리번호가 같아도 좌표·주소·설치정보가 바뀌었으면 그 항목이 담긴다.
     * 선택 항목은 행에 값이 있을 때만 비교한다 — toPoint의 유지 규칙과 어긋나면 보여 준 것과 실제가 갈린다.
     * BigDecimal은 자릿수 차이를 무시하려 compareTo로 본다.
     *
     * 등록과 미리보기가 이 판정을 함께 쓴다 — 따로 두면 보여 준 것과 실제로 벌어지는 일이 어긋난다.
     */
    public static List<FieldChange> changes(ControlPoint p, Row row) {
        List<FieldChange> changes = new ArrayList<>();
        addIfDiffers(changes, "관리번호", p.getPointNo(), row.pointNo());
        addIfDiffers(changes, "좌표계", p.getTm().crs(), row.tm().crs());
        if (p.getTm().northing().compareTo(row.tm().northing()) != 0) {
            changes.add(new FieldChange("X좌표", p.getTm().northing().toPlainString(), row.tm().northing().toPlainString()));
        }
        if (p.getTm().easting().compareTo(row.tm().easting()) != 0) {
            changes.add(new FieldChange("Y좌표", p.getTm().easting().toPlainString(), row.tm().easting().toPlainString()));
        }
        addIfDiffers(changes, "경위도", geoText(p.getGeo()), geoText(row.geo()));
        if (row.regionCode() != null) {
            addIfDiffers(changes, "법정동코드", p.getRegionCode(), row.regionCode());
        }
        if (row.regionName() != null) {
            addIfDiffers(changes, "소재지", p.getRegionName(), row.regionName());
        }
        if (row.address() != null) {
            addIfDiffers(changes, "상세주소", p.getAddress(), row.address());
        }
        if (row.markerMaterial() != null) {
            addIfDiffers(changes, "표지재질", p.getMarkerMaterial(), row.markerMaterial());
        }
        if (row.installType() != null) {
            addIfDiffers(changes, "설치구분", p.getInstallType(), row.installType());
        }
        if (row.installedDate() != null) {
            addIfDiffers(changes, "설치일자", p.getInstalledDate(), row.installedDate());
        }
        if (row.traverse() != null) {
            addIfDiffers(changes, "도선정보", p.getTraverse(), row.traverse());
        }
        // 시드 조사는 등록과 같은 규칙으로 고른 뒤에 견준다 — 보여 준 것과 실제로 벌어지는 일이 어긋나지 않게
        SeedSurvey seed = seedSurvey(p, row);
        addIfDiffers(changes, "최종조사내용", p.getLastSurveyResult(), seed.result());
        addIfDiffers(changes, "최종조사일", p.getLastSurveyedOn(), seed.surveyedOn());
        return List.copyOf(changes);
    }

    private static void addIfDiffers(List<FieldChange> changes, String field, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            changes.add(new FieldChange(field, text(before), text(after)));
        }
    }

    /**
     * 화면이 그대로 보여 줄 값.
     * 열거형·값 객체를 그대로 문자열로 만들면 내부 이름(BESSEL_CENTRAL, TraverseInfo[grade=null…])이 화면에 나온다.
     */
    private static String text(Object value) {
        return switch (value) {
            case null -> "";
            case CoordinateSystem crs -> crs.getDisplayName();
            case MarkerMaterial material -> material.getDisplayName();
            case InstallType install -> install.getDisplayName();
            case TraverseInfo traverse -> traverseText(traverse);
            default -> String.valueOf(value);
        };
    }

    /** 도선 정보 한 줄 — 적힌 항목만 이어 붙인다. */
    private static String traverseText(TraverseInfo traverse) {
        List<String> parts = new ArrayList<>();
        if (traverse.grade() != null) parts.add("등급 " + traverse.grade());
        if (traverse.lineName() != null) parts.add("도선 " + traverse.lineName());
        if (traverse.lineNo() != null) parts.add("도호 " + traverse.lineNo());
        if (Boolean.TRUE.equals(traverse.intersection())) parts.add("교차점");
        return parts.isEmpty() ? "" : String.join(" · ", parts);
    }

    private static String geoText(GeoCoordinate geo) {
        // 소수 구분자가 쉼표인 로케일로 서버가 떠도 값이 흔들리지 않게 고정한다
        return String.format(Locale.ROOT, "%.6f, %.6f", geo.longitude(), geo.latitude());
    }

}
