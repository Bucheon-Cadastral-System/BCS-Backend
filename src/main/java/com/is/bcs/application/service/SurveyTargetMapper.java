package com.is.bcs.application.service;

import com.is.bcs.application.port.out.file.Table;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TraverseInfo;
import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import com.is.bcs.domain.survey.SurveyResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 대상지 표의 각 행을 도메인 값으로 읽는다.
 * 열은 위치가 아니라 이름으로 찾는다 — 고객사가 기본 양식에 열을 더하거나 빼도 그대로 읽히게.
 *
 * 별칭은 한글 서식에서 실제로 확인한 표기만 등록한다.
 * 특히 5174 정의서의 영문 컬럼(POINT_X 등)은 X가 동방향이라 이 서식의 X좌표(북방향)와 축이 반대이므로 섞지 않는다.
 */
public final class SurveyTargetMapper {

    /** 표 한 행 — 기준점 마스터 필드 + 기존조사 이력. */
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
            String note
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

    private SurveyTargetMapper() {
    }

    public static List<Row> map(Table table) {
        Map<String, Integer> columns = columnIndex(table.headers());

        List<String> missing = REQUIRED_COLUMNS.stream().filter(c -> !columns.containsKey(normalize(c))).toList();
        if (!missing.isEmpty()) {
            // 데이터 행이 없어도(빈 목록 → 빈 조사) 조용히 성공하지 않도록 헤더를 먼저 검증한다
            throw new InvalidControlPointException("필수 열이 없습니다: " + String.join(", ", missing));
        }

        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < table.rows().size(); i++) {
            rows.add(mapRow(table.rows().get(i), columns, i + 2)); // 헤더가 1행이므로 데이터는 2행부터
        }
        return rows;
    }

    /** 정규화한 열 이름 → 위치. 별칭은 표준 이름으로 접어 넣는다. */
    private static Map<String, Integer> columnIndex(List<String> headers) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String key = normalize(headers.get(i));
            if (key.isEmpty()) {
                continue;
            }
            index.putIfAbsent(ALIASES.getOrDefault(key, key), i);
        }
        return index;
    }

    /** 띄어쓰기·괄호·기호는 표기 흔들림일 뿐이므로 지우고 비교한다. */
    private static String normalize(String header) {
        return header.replaceAll("[\\s()\\[\\]_.\\-/]", "").toLowerCase();
    }

    private static Row mapRow(List<String> cells, Map<String, Integer> columns, int rowNum) {
        String regionRaw = cell(cells, columns, REGION);
        String regionCode = null;
        String regionName = regionRaw;
        if (regionRaw != null && regionRaw.contains("-")) {
            int at = regionRaw.indexOf('-');
            regionCode = regionRaw.substring(0, at);
            regionName = regionRaw.substring(at + 1);
        }

        return new Row(
                cell(cells, columns, POINT_NO),
                pointType(cell(cells, columns, TYPE), rowNum),
                cell(cells, columns, NAME),
                crs(cell(cells, columns, CRS), rowNum),
                decimal(cell(cells, columns, NORTHING), NORTHING, rowNum),
                decimal(cell(cells, columns, EASTING), EASTING, rowNum),
                optionalNumber(cell(cells, columns, LONGITUDE), "경도", rowNum),
                optionalNumber(cell(cells, columns, LATITUDE), "위도", rowNum),
                regionCode,
                regionName,
                cell(cells, columns, ADDRESS),
                material(cell(cells, columns, MATERIAL), rowNum),
                install(cell(cells, columns, INSTALL_TYPE), rowNum),
                date(cell(cells, columns, INSTALLED_DATE), INSTALLED_DATE, rowNum),
                traverse(cells, columns, rowNum),
                priorResult(cell(cells, columns, PRIOR_RESULT), rowNum),
                date(cell(cells, columns, PRIOR_SURVEY_DATE), PRIOR_SURVEY_DATE, rowNum),
                cell(cells, columns, TARGET_NOTE)
        );
    }

    private static TraverseInfo traverse(List<String> cells, Map<String, Integer> columns, int rowNum) {
        String grade = cell(cells, columns, TRAVERSE_GRADE);
        String lineName = cell(cells, columns, TRAVERSE_NAME);
        String lineNo = cell(cells, columns, TRAVERSE_NO);
        Boolean intersection = intersection(cell(cells, columns, INTERSECTION), rowNum);
        if (grade == null && lineName == null && lineNo == null && intersection == null) {
            return null;
        }
        return new TraverseInfo(grade, lineName, lineNo, intersection);
    }

    private static PointType pointType(String value, int rowNum) {
        return switch (require(value, TYPE, rowNum)) {
            case "도근점", "지적도근점" -> PointType.DOGEUN;
            case "삼각보조점", "지적삼각보조점" -> PointType.TRIANGULATION_AUX;
            case "삼각점", "지적삼각점" -> PointType.TRIANGULATION;
            default -> throw unknown(TYPE, value, rowNum);
        };
    }

    private static CoordinateSystem crs(String value, int rowNum) {
        // 부천 현행 성과는 세계측지계 중부원점 — 다른 좌표계구분이 나오면 데이터 확인이 먼저다
        return switch (require(value, CRS, rowNum)) {
            case "세계" -> CoordinateSystem.GRS80_CENTRAL;
            default -> throw unknown(CRS, value, rowNum);
        };
    }

    private static MarkerMaterial material(String value, int rowNum) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "표석" -> MarkerMaterial.STONE;
            case "철재" -> MarkerMaterial.STEEL;
            default -> throw unknown(MATERIAL, value, rowNum);
        };
    }

    private static InstallType install(String value, int rowNum) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "설치" -> InstallType.INSTALLED;
            case "재설", "재설치" -> InstallType.REINSTALLED;
            default -> throw unknown(INSTALL_TYPE, value, rowNum);
        };
    }

    private static Boolean intersection(String value, int rowNum) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "도근점" -> Boolean.FALSE;
            case "교차점" -> Boolean.TRUE;
            default -> throw unknown(INTERSECTION, value, rowNum);
        };
    }

    private static SurveyResult priorResult(String value, int rowNum) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "완전" -> SurveyResult.INTACT;
            case "망실" -> SurveyResult.LOST;
            case "기타" -> SurveyResult.ETC;
            default -> throw unknown(PRIOR_RESULT, value, rowNum);
        };
    }

    private static BigDecimal decimal(String value, String field, int rowNum) {
        try {
            return new BigDecimal(require(value, field, rowNum));
        } catch (NumberFormatException e) {
            throw unknown(field, value, rowNum);
        }
    }

    /** 경위도는 기본 양식에 없는 열이라 비어 있을 수 있다. 없으면 성과 좌표에서 파생한다. */
    private static Double optionalNumber(String value, String field, int rowNum) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw unknown(field, value, rowNum);
        }
    }

    private static LocalDate date(String value, String field, int rowNum) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            throw unknown(field, value, rowNum);
        }
    }

    /** 필수 열은 map 에서 이미 검증했으므로, 여기서 없는 열은 선택 항목이다 — 거부하지 않고 비운다. */
    private static String cell(List<String> cells, Map<String, Integer> columns, String column) {
        Integer at = columns.get(normalize(column));
        if (at == null || at >= cells.size()) {
            return null;
        }
        String value = cells.get(at).trim();
        return value.isEmpty() ? null : value;
    }

    private static String require(String value, String field, int rowNum) {
        if (value == null) {
            throw new InvalidControlPointException(rowNum + "행: " + field + "이(가) 비어 있습니다.");
        }
        return value;
    }

    private static InvalidControlPointException unknown(String field, String value, int rowNum) {
        return new InvalidControlPointException(rowNum + "행: 알 수 없는 " + field + ": " + value);
    }
}
