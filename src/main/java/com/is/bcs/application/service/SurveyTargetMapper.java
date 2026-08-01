package com.is.bcs.application.service;

import com.is.bcs.application.port.out.file.Table;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TraverseInfo;
import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import com.is.bcs.domain.survey.ExtraColumn;
import com.is.bcs.domain.survey.SurveyResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 대상지 표의 각 행을 도메인 값으로 읽는다.
 * 열은 위치가 아니라 이름으로 찾는다 — 고객사가 기본 양식에 열을 더하거나 빼도 그대로 읽히게.
 *
 * 기본 양식의 열만 도메인 필드로 해석하고, 그 밖의 열은 뜻을 묻지 않고 이름·값 그대로 넘긴다(extras).
 * 고객사가 "필요에 따라 열을 추가해서 관리한다"고 밝힌 서식이라, 아는 열만 남기고 버리면 올린 파일이 손실된다.
 *
 * 별칭은 한글 서식에서 실제로 확인한 표기만 등록한다.
 * 특히 5174 정의서의 영문 컬럼(POINT_X 등)은 X가 동방향이라 이 서식의 X좌표(북방향)와 축이 반대이므로 섞지 않는다.
 */
public final class SurveyTargetMapper {

    /** 표 한 행 — 기준점 마스터 필드 + 기존조사 이력 + 해석하지 않고 보관하는 열. */
    public record Row(
            String pointNo,
            PointType type,
            String name,
            CoordinateSystem crs,
            BigDecimal northing,
            BigDecimal easting,
            Double longitude,
            Double latitude,
            String regionCode,
            String regionName,
            String address,
            MarkerMaterial markerMaterial,
            InstallType installType,
            LocalDate installedDate,
            TraverseInfo traverse,
            SurveyResult priorResult,
            LocalDate priorSurveyDate,
            String note,
            List<ExtraColumn> extras
    ) {
    }

    /** 표준 열 이름 — 오류 메시지에도 이 이름으로 나간다. */
    private static final String POINT_NO = "기준점번호";
    private static final String TYPE = "종류";
    private static final String NAME = "기준점명";
    private static final String CRS = "좌표계구분";
    private static final String NORTHING = "X좌표";
    private static final String EASTING = "Y좌표";
    private static final String LONGITUDE = "경도(X)";
    private static final String LATITUDE = "위도(Y)";
    private static final String REGION = "토지소재지";
    private static final String ADDRESS = "상세주소";
    private static final String MATERIAL = "표지재질";
    private static final String TRAVERSE_GRADE = "도선등급";
    private static final String TRAVERSE_NAME = "도선명";
    private static final String TRAVERSE_NO = "도호";
    private static final String INTERSECTION = "교차구분";
    private static final String INSTALL_TYPE = "설치구분";
    private static final String INSTALLED_DATE = "설치일자";
    private static final String PRIOR_RESULT = "기존조사내용";
    private static final String PRIOR_SURVEY_DATE = "기존조사일";
    private static final String TARGET_NOTE = "조사대상여부";

    private static final List<String> REQUIRED_COLUMNS =
            List.of(POINT_NO, TYPE, NAME, CRS, NORTHING, EASTING);

    /** 이 매퍼가 읽을 줄 아는 항목 전부 — 여기 없는 열은 무시된다. */
    private static final List<String> KNOWN_COLUMNS = List.of(
            POINT_NO, TYPE, NAME, CRS, NORTHING, EASTING, LONGITUDE, LATITUDE, REGION, ADDRESS,
            MATERIAL, TRAVERSE_GRADE, TRAVERSE_NAME, TRAVERSE_NO, INTERSECTION,
            INSTALL_TYPE, INSTALLED_DATE, PRIOR_RESULT, PRIOR_SURVEY_DATE, TARGET_NOTE);

    private static final Map<String, String> STANDARD_BY_NORMALIZED = KNOWN_COLUMNS.stream()
            .collect(Collectors.toUnmodifiableMap(SurveyTargetMapper::normalize, column -> column));

    /**
     * 같은 항목을 가리키는 다른 표기 — 키·값 모두 정규화한 형태로 둔다(조회도 정규화한 이름으로 하므로).
     * 5자에서 잘린 이름(기존조사내·조사대상여)은 고객사 내보내기가 실제로 그렇게 준다.
     */
    private static final Map<String, String> ALIASES = Map.of(
            normalize("관리번호"), normalize(POINT_NO),
            normalize("기존조사내"), normalize(PRIOR_RESULT),
            normalize("조사대상여"), normalize(TARGET_NOTE),
            normalize("경도"), normalize(LONGITUDE),
            normalize("위도"), normalize(LATITUDE));

    /**
     * 읽은 행과 함께, 어떻게 읽었는지(열 매핑)와 읽지 못한 행(오류)을 돌려준다.
     * 오류에서 멈추지 않고 끝까지 훑는다 — 담당자가 파일을 한 번에 고칠 수 있어야 한다.
     */
    public record MappingResult(List<Row> rows, ColumnMapping columns, List<RowError> errors) {

        /** 표의 데이터 행 수 — 읽힌 행과 오류 행을 합한 값. */
        public int totalRows() {
            return rows.size() + errors.size();
        }
    }

    /**
     * 파일의 열 이름이 어떤 항목으로 읽혔는지, 그리고 해석하지 않고 값만 보관한 열이 무엇인지.
     * 순서는 파일에 적힌 그대로 둔다.
     */
    public record ColumnMapping(Map<String, String> recognized, List<String> extra) {
    }

    public record RowError(int row, String message) {
    }

    private SurveyTargetMapper() {
    }

    public static MappingResult map(Table table) {
        Map<String, Integer> columns = columnIndex(table.headers());

        List<String> missing = REQUIRED_COLUMNS.stream().filter(c -> !columns.containsKey(normalize(c))).toList();
        if (!missing.isEmpty()) {
            // 파일 전체를 읽을 수 없는 상태라 행 오류로 표현할 수 없다 — 데이터 행이 없어도 여기서 멈춘다
            throw new InvalidControlPointException("필수 열이 없습니다: " + String.join(", ", missing));
        }

        // 이름 있는 열은 해석하거나 보관하거나 둘 중 하나다.
        // 같은 항목으로 풀리는 열이 둘이면 뒤엣것은 해석에 쓰이지 않으므로 보관 쪽으로 넘겨 값이 사라지지 않게 한다.
        Set<Integer> interpreted = Set.copyOf(columns.values());
        List<Integer> extraPositions = extraPositions(table.headers(), interpreted);
        ColumnMapping mapping = new ColumnMapping(
                recognizedColumns(table.headers(), interpreted),
                extraPositions.stream().map(table.headers()::get).toList());

        List<Row> rows = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        Set<String> seenPointNos = new HashSet<>();
        Set<String> seenPoints = new HashSet<>();
        for (int i = 0; i < table.rows().size(); i++) {
            // 제목 행·빈 줄을 건너뛰고 헤더를 찾으므로 순서가 아니라 원본 줄 번호로 알린다
            int rowNumber = table.sourceRowNumbers().get(i);
            try {
                Row row = mapRow(table.rows().get(i), columns, table.headers(), extraPositions);
                String duplicated = duplicated(row, seenPointNos, seenPoints);
                if (duplicated == null) {
                    rows.add(row);
                } else {
                    errors.add(new RowError(rowNumber, duplicated));
                }
            } catch (InvalidControlPointException e) {
                // 한 행이 잘못됐다고 멈추면 담당자가 고치고 올리기를 반복해야 한다 — 모아서 한 번에 보여준다
                errors.add(new RowError(rowNumber, e.getMessage()));
            }
        }
        return new MappingResult(List.copyOf(rows), mapping, List.copyOf(errors));
    }

    /**
     * 한 파일 안에 같은 기준점이 두 번 있으면 그 사유를, 없으면 null.
     * 그대로 두면 뒤 행이 앞 행의 성과를 덮어쓰고, 같은 조사에 같은 대상이 두 번 등록돼 저장 단계에서 제약에 걸린다.
     */
    private static String duplicated(Row row, Set<String> pointNos, Set<String> points) {
        String pointKey = row.type() + "|" + row.name();
        // 판정을 먼저 하고 통과한 행만 등록한다 — 거부한 행의 값을 남기면 뒤 행이 그 값 때문에 잘못 걸린다
        if (pointNos.contains(row.pointNo())) {
            return "같은 관리번호가 앞 행에 이미 있습니다: " + row.pointNo();
        }
        if (points.contains(pointKey)) {
            return "같은 기준점이 앞 행에 이미 있습니다: " + row.name();
        }
        pointNos.add(row.pointNo());
        points.add(pointKey);
        return null;
    }

    /** 실제로 해석에 쓰인 열 — 파일의 열 이름 → 표준 이름. 순서는 파일에 적힌 그대로 둔다. */
    private static Map<String, String> recognizedColumns(List<String> headers, Set<Integer> interpreted) {
        Map<String, String> recognized = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            if (interpreted.contains(i)) {
                recognized.putIfAbsent(headers.get(i), standardOf(headers.get(i)));
            }
        }
        return Collections.unmodifiableMap(recognized);
    }

    /** 값만 보관할 열의 위치 — 행마다 헤더를 다시 훑지 않도록 한 번만 구한다. */
    private static List<Integer> extraPositions(List<String> headers, Set<Integer> interpreted) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            // 이름이 없는 열은 무엇으로 되살릴지 알 수 없어 보관 대상에서 뺀다
            if (!interpreted.contains(i) && !normalize(headers.get(i)).isEmpty()) {
                positions.add(i);
            }
        }
        return List.copyOf(positions);
    }

    /** 이 열이 읽어 들일 표준 항목 — 이름이 비었거나 사전에 없으면 null. */
    private static String standardOf(String header) {
        String key = normalize(header);
        return key.isEmpty() ? null : STANDARD_BY_NORMALIZED.get(resolve(key));
    }

    /**
     * 표준 항목(정규화한 이름) → 위치. 별칭은 표준 이름으로 접어 넣는다.
     * 같은 항목으로 풀리는 열이 둘이면 앞엣것만 쓴다 — 뒤엣것은 해석에 쓰이지 않으므로 보관 대상이 된다.
     */
    private static Map<String, Integer> columnIndex(List<String> headers) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String standard = standardOf(headers.get(i));
            if (standard != null) {
                index.putIfAbsent(normalize(standard), i);
            }
        }
        return index;
    }

    private static String resolve(String normalizedHeader) {
        return ALIASES.getOrDefault(normalizedHeader, normalizedHeader);
    }

    /** 띄어쓰기·괄호·기호는 표기 흔들림일 뿐이므로 지우고 비교한다. */
    private static String normalize(String header) {
        return header.replaceAll("[\\s()\\[\\]_.\\-/]", "").toLowerCase();
    }

    private static Row mapRow(
            List<String> cells, Map<String, Integer> columns, List<String> headers, List<Integer> extraPositions) {
        // 소재지는 "10300-춘의동" 처럼 법정동 코드와 이름이 붙어 온다.
        // 앞이 숫자일 때만 나눈다 — "춘의동 102-16" 같은 지번을 코드와 이름으로 잘못 가르지 않게.
        String regionRaw = cell(cells, columns, REGION);
        String regionCode = null;
        String regionName = regionRaw;
        if (regionRaw != null) {
            int at = regionRaw.indexOf('-');
            String prefix = at > 0 ? regionRaw.substring(0, at) : "";
            if (prefix.chars().allMatch(Character::isDigit) && !prefix.isEmpty()) {
                regionCode = prefix;
                regionName = regionRaw.substring(at + 1);
            }
        }

        return new Row(
                // 열이 있어도 칸이 비어 있을 수 있다 — 등록 단계에서 터뜨리지 않고 여기서 행 오류로 만든다
                require(cell(cells, columns, POINT_NO), POINT_NO),
                pointType(cell(cells, columns, TYPE)),
                require(cell(cells, columns, NAME), NAME),
                crs(cell(cells, columns, CRS)),
                decimal(cell(cells, columns, NORTHING), NORTHING),
                decimal(cell(cells, columns, EASTING), EASTING),
                optionalNumber(cell(cells, columns, LONGITUDE), "경도"),
                optionalNumber(cell(cells, columns, LATITUDE), "위도"),
                regionCode,
                regionName,
                cell(cells, columns, ADDRESS),
                material(cell(cells, columns, MATERIAL)),
                install(cell(cells, columns, INSTALL_TYPE)),
                date(cell(cells, columns, INSTALLED_DATE), INSTALLED_DATE),
                traverse(cells, columns),
                priorResult(cell(cells, columns, PRIOR_RESULT)),
                date(cell(cells, columns, PRIOR_SURVEY_DATE), PRIOR_SURVEY_DATE),
                cell(cells, columns, TARGET_NOTE),
                extras(cells, headers, extraPositions)
        );
    }

    /** 해석하지 않는 열은 이름과 값을 적힌 그대로 옮긴다 — 뜻을 모르므로 형식 검사도 하지 않는다. */
    private static List<ExtraColumn> extras(List<String> cells, List<String> headers, List<Integer> positions) {
        List<ExtraColumn> extras = new ArrayList<>(positions.size());
        for (int position : positions) {
            extras.add(new ExtraColumn(headers.get(position), valueAt(cells, position)));
        }
        return List.copyOf(extras);
    }

    private static TraverseInfo traverse(List<String> cells, Map<String, Integer> columns) {
        String grade = cell(cells, columns, TRAVERSE_GRADE);
        String lineName = cell(cells, columns, TRAVERSE_NAME);
        String lineNo = cell(cells, columns, TRAVERSE_NO);
        Boolean intersection = intersection(cell(cells, columns, INTERSECTION));
        if (grade == null && lineName == null && lineNo == null && intersection == null) {
            return null;
        }
        return new TraverseInfo(grade, lineName, lineNo, intersection);
    }

    private static PointType pointType(String value) {
        return switch (require(value, TYPE)) {
            case "도근점", "지적도근점" -> PointType.DOGEUN;
            case "삼각보조점", "지적삼각보조점" -> PointType.TRIANGULATION_AUX;
            case "삼각점", "지적삼각점" -> PointType.TRIANGULATION;
            default -> throw unknown(TYPE, value);
        };
    }

    private static CoordinateSystem crs(String value) {
        // 부천 현행 성과는 세계측지계 중부원점 — 다른 좌표계구분이 나오면 데이터 확인이 먼저다
        return switch (require(value, CRS)) {
            case "세계" -> CoordinateSystem.GRS80_CENTRAL;
            default -> throw unknown(CRS, value);
        };
    }

    private static MarkerMaterial material(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "표석" -> MarkerMaterial.STONE;
            case "철재" -> MarkerMaterial.STEEL;
            default -> throw unknown(MATERIAL, value);
        };
    }

    private static InstallType install(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "설치" -> InstallType.INSTALLED;
            case "재설", "재설치" -> InstallType.REINSTALLED;
            default -> throw unknown(INSTALL_TYPE, value);
        };
    }

    private static Boolean intersection(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "도근점" -> Boolean.FALSE;
            case "교차점" -> Boolean.TRUE;
            default -> throw unknown(INTERSECTION, value);
        };
    }

    private static SurveyResult priorResult(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "완전" -> SurveyResult.INTACT;
            case "망실" -> SurveyResult.LOST;
            case "기타" -> SurveyResult.ETC;
            default -> throw unknown(PRIOR_RESULT, value);
        };
    }

    private static BigDecimal decimal(String value, String field) {
        try {
            return new BigDecimal(require(value, field));
        } catch (NumberFormatException e) {
            throw unknown(field, value);
        }
    }

    /** 경위도는 기본 양식에 없는 열이라 비어 있을 수 있다. 없으면 성과 좌표에서 파생한다. */
    private static Double optionalNumber(String value, String field) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw unknown(field, value);
        }
    }

    private static LocalDate date(String value, String field) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw unknown(field, value);
        }
    }

    /** 필수 열은 map 에서 이미 검증했으므로, 여기서 없는 열은 선택 항목이다 — 거부하지 않고 비운다. */
    private static String cell(List<String> cells, Map<String, Integer> columns, String column) {
        Integer at = columns.get(normalize(column));
        return at == null ? null : valueAt(cells, at);
    }

    /** 헤더보다 짧은 행이면 뒤쪽 열은 비어 있는 것으로 본다. */
    private static String valueAt(List<String> cells, int position) {
        if (position >= cells.size()) {
            return null;
        }
        String value = cells.get(position).trim();
        return value.isEmpty() ? null : value;
    }

    private static String require(String value, String field) {
        if (value == null) {
            throw new InvalidControlPointException(field + "이(가) 비어 있습니다.");
        }
        return value;
    }

    private static InvalidControlPointException unknown(String field, String value) {
        return new InvalidControlPointException("알 수 없는 " + field + ": " + value);
    }
}
