package com.is.bcs.application.service;

import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TraverseInfo;
import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import com.is.bcs.domain.survey.SurveyResult;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 굴착협의 대상지 CSV(EUC-KR) 파서.
 * 축 관례 주의: 원본 X좌표=북방향(northing), Y좌표=동방향(easting).
 * 어휘(종류·좌표계·재질·설치·교차·조사결과)는 등록된 값만 허용하고, 알 수 없는 값은 행 번호와 함께 거부한다.
 */
public final class ExcavationCsvParser {

    private static final Charset EUC_KR = Charset.forName("EUC-KR");

    /** CSV 한 행 — 기준점 마스터 필드 + 기존조사 이력. */
    public record Row(
            String pointNo,
            PointType type,
            String name,
            CoordinateSystem crs,
            BigDecimal northing,
            BigDecimal easting,
            double longitude,
            double latitude,
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

    private ExcavationCsvParser() {
    }

    public static List<Row> parse(byte[] content) {
        List<String> lines = new String(content, EUC_KR).lines().filter(l -> !l.isBlank()).toList();
        if (lines.isEmpty()) {
            throw new InvalidControlPointException("CSV에 헤더가 없습니다.");
        }

        Map<String, Integer> header = headerIndex(splitCsv(lines.get(0)));
        List<Row> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            rows.add(parseRow(splitCsv(lines.get(i)), header, i + 1));
        }
        return rows;
    }

    private static Row parseRow(List<String> cells, Map<String, Integer> header, int rowNum) {
        String pointNo = cell(cells, header, "기준점번호");
        String regionRaw = cell(cells, header, "토지소재지");
        String regionCode = null;
        String regionName = regionRaw;
        if (regionRaw != null && regionRaw.contains("-")) {
            int at = regionRaw.indexOf('-');
            regionCode = regionRaw.substring(0, at);
            regionName = regionRaw.substring(at + 1);
        }

        return new Row(
                pointNo,
                pointType(cell(cells, header, "종류"), rowNum),
                cell(cells, header, "기준점명"),
                crs(cell(cells, header, "좌표계구분"), rowNum),
                decimal(cell(cells, header, "X좌표"), "X좌표", rowNum),
                decimal(cell(cells, header, "Y좌표"), "Y좌표", rowNum),
                number(cell(cells, header, "경도(X)"), "경도", rowNum),
                number(cell(cells, header, "위도(Y)"), "위도", rowNum),
                regionCode,
                regionName,
                cell(cells, header, "상세주소"),
                material(cell(cells, header, "표지재질"), rowNum),
                install(cell(cells, header, "설치구분"), rowNum),
                date(cell(cells, header, "설치일자"), "설치일자", rowNum),
                traverse(cells, header, rowNum),
                priorResult(cell(cells, header, "기존조사내"), rowNum),
                date(cell(cells, header, "기존조사일"), "기존조사일", rowNum),
                cell(cells, header, "조사대상여")
        );
    }

    private static TraverseInfo traverse(List<String> cells, Map<String, Integer> header, int rowNum) {
        String grade = cell(cells, header, "도선등급");
        String lineName = cell(cells, header, "도선명");
        String lineNo = cell(cells, header, "도호");
        Boolean intersection = intersection(cell(cells, header, "교차구분"), rowNum);
        if (grade == null && lineName == null && lineNo == null && intersection == null) {
            return null;
        }
        return new TraverseInfo(grade, lineName, lineNo, intersection);
    }

    private static PointType pointType(String value, int rowNum) {
        return switch (require(value, "종류", rowNum)) {
            case "도근점", "지적도근점" -> PointType.DOGEUN;
            case "삼각보조점", "지적삼각보조점" -> PointType.TRIANGULATION_AUX;
            case "삼각점", "지적삼각점" -> PointType.TRIANGULATION;
            default -> throw unknown("종류", value, rowNum);
        };
    }

    private static CoordinateSystem crs(String value, int rowNum) {
        // 부천 현행 성과는 세계측지계 중부원점 — 다른 좌표계구분이 나오면 데이터 확인이 먼저다
        return switch (require(value, "좌표계구분", rowNum)) {
            case "세계" -> CoordinateSystem.GRS80_CENTRAL;
            default -> throw unknown("좌표계구분", value, rowNum);
        };
    }

    private static MarkerMaterial material(String value, int rowNum) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "표석" -> MarkerMaterial.STONE;
            case "철재" -> MarkerMaterial.STEEL;
            default -> throw unknown("표지재질", value, rowNum);
        };
    }

    private static InstallType install(String value, int rowNum) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "설치" -> InstallType.INSTALLED;
            case "재설", "재설치" -> InstallType.REINSTALLED;
            default -> throw unknown("설치구분", value, rowNum);
        };
    }

    private static Boolean intersection(String value, int rowNum) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "도근점" -> Boolean.FALSE;
            case "교차점" -> Boolean.TRUE;
            default -> throw unknown("교차구분", value, rowNum);
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
            default -> throw unknown("기존조사내용", value, rowNum);
        };
    }

    private static BigDecimal decimal(String value, String field, int rowNum) {
        try {
            return new BigDecimal(require(value, field, rowNum));
        } catch (NumberFormatException e) {
            throw unknown(field, value, rowNum);
        }
    }

    private static double number(String value, String field, int rowNum) {
        try {
            return Double.parseDouble(require(value, field, rowNum));
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

    private static String require(String value, String field, int rowNum) {
        if (value == null) {
            throw new InvalidControlPointException(rowNum + "행: " + field + "이(가) 비어 있습니다.");
        }
        return value;
    }

    private static InvalidControlPointException unknown(String field, String value, int rowNum) {
        return new InvalidControlPointException(rowNum + "행: 알 수 없는 " + field + ": " + value);
    }

    private static Map<String, Integer> headerIndex(List<String> headerCells) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headerCells.size(); i++) {
            index.put(headerCells.get(i).trim(), i);
        }
        return index;
    }

    private static String cell(List<String> cells, Map<String, Integer> header, String name) {
        Integer at = header.get(name);
        if (at == null) {
            throw new InvalidControlPointException("CSV 헤더에 '" + name + "' 컬럼이 없습니다.");
        }
        if (at >= cells.size()) {
            return null;
        }
        String value = cells.get(at).trim();
        return value.isEmpty() ? null : value;
    }

    /** 따옴표 안 콤마를 보존하는 CSV 한 줄 분해. */
    private static List<String> splitCsv(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cells.add(current.toString());
        return cells;
    }
}
