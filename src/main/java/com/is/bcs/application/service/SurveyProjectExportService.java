package com.is.bcs.application.service;

import com.is.bcs.application.dto.LastSurveySummary;
import com.is.bcs.application.dto.SurveyProjectExportFile;
import com.is.bcs.application.dto.SurveyRecordSummary;
import com.is.bcs.application.port.in.survey.ExportSurveyProjectUseCase;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.file.Table;
import com.is.bcs.application.port.out.file.TableWriter;
import com.is.bcs.application.port.out.survey.LoadSurveyProjectPort;
import com.is.bcs.application.port.out.survey.LoadSurveyRecordPort;
import com.is.bcs.application.port.out.survey.LoadSurveyTargetPort;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.exception.SurveyProjectNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 조사의 대상 기준점을 파일 한 장으로 내보낸다.
 *
 * <p>열 이름과 어휘를 대상지 파일 읽기({@link ImportFileMapper})와 같게 맞춘다. 내보낸 파일을 그대로 다시
 * 올려 다음 회차를 열 수 있어야 하기 때문이다. 그래서 이 클래스가 쓰는 표기가 바뀌면 읽는 쪽도 함께 바뀐다.
 *
 * <p>맨 뒤 네 열(최종조사)은 읽기가 요구하지 않는 값이다. 그 점이 지금 어떤 상태로 남아 있는지를 파일 하나로
 * 보여 주기 위한 것이라, 화면의 점 상세가 세우는 최종조사와 같은 값을 같은 규칙으로 고른다.
 */
@Service
@RequiredArgsConstructor
public class SurveyProjectExportService implements ExportSurveyProjectUseCase {

    /**
     * 내보내는 열 — 앞 열한 개는 대상지 파일이 요구하는 열이고 그 차례도 고객사 서식과 같다.
     * 경위도는 성과 좌표 옆에 같은 축 차례로 붙이고, 최종조사 네 열은 맨 뒤에 둔다.
     */
    private static final List<String> HEADERS = List.of(
            "종류", "기준점명", "기준점번호", "좌표계구분", "X좌표", "Y좌표", "경도(X)", "위도(Y)",
            "토지소재지", "상세주소", "설치일자", "기존조사내용", "기존조사일",
            "최종조사내용", "최종조사일자", "최종조사원", "비고");

    /** 저장 이름에 쓸 수 없는 글자 — 운영체제마다 다르므로 어디서나 막히는 것을 모두 바꾼다. */
    private static final Pattern UNSAFE_FILE_NAME = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]");
    /** 이름의 숫자는 자릿수가 아니라 값으로 견준다 — 화면 목록과 같은 차례로 선다. */
    private static final Pattern DIGITS = Pattern.compile("(\\d+)");
    private static final Collator KOREAN = Collator.getInstance(Locale.KOREAN);
    /** 경위도 자릿수 — 파생값이라 성과처럼 자릿수를 못박을 필요는 없고, 표기가 흔들리지 않을 만큼만 자른다. */
    private static final int GEO_SCALE = 8;

    private final LoadSurveyProjectPort loadSurveyProjectPort;
    private final LoadSurveyTargetPort loadSurveyTargetPort;
    private final LoadSurveyRecordPort loadSurveyRecordPort;
    private final LoadControlPointPort loadControlPointPort;
    private final TableWriter tableWriter;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public SurveyProjectExportFile export(Long projectId) {
        SurveyProject project = loadSurveyProjectPort.findProjectById(projectId)
                .orElseThrow(() -> new SurveyProjectNotFoundException("조사를 찾을 수 없습니다: " + projectId));
        List<Long> pointIds = loadSurveyTargetPort.findPointIdsByProjectId(projectId);
        List<ControlPoint> points = new ArrayList<>(loadControlPointPort.findAllByIds(pointIds));
        points.sort(Comparator.comparing(ControlPoint::getType).thenComparing(ControlPoint::getName, byName()));

        Map<Long, SurveyRecord> thisRound = loadSurveyRecordPort.findRecordSummariesByProjectId(projectId).stream()
                .map(SurveyRecordSummary::record)
                .collect(Collectors.toMap(SurveyRecord::getPointId, Function.identity()));
        Map<Long, SurveyRecordSummary> latest = loadSurveyRecordPort
                .findLatestRecordSummariesByPointIds(pointIds).stream()
                .collect(Collectors.toMap(summary -> summary.record().getPointId(), Function.identity()));

        List<List<String>> rows = points.stream()
                .map(point -> row(point, thisRound.get(point.getId()), lastSurvey(point, latest.get(point.getId()))))
                .toList();
        byte[] content = tableWriter.write("대상 기준점", new Table(HEADERS, rows));
        return new SurveyProjectExportFile(content, fileName(project.getName()));
    }

    /** 화면의 점 상세와 같은 최종조사 — 기준점이 든 시드 조사와 조사기록 중 날짜가 늦은 쪽. */
    private LastSurveySummary lastSurvey(ControlPoint point, SurveyRecordSummary latest) {
        LastSurveySummary seed = new LastSurveySummary(
                point.getLastSurveyResult(), point.getLastSurveyedOn(), null, null);
        return latest == null
                ? seed
                : LastSurveySummary.later(seed, LastSurveySummary.of(latest.record(), latest.surveyorName(), clock.getZone()));
    }

    private List<String> row(ControlPoint point, SurveyRecord thisRound, LastSurveySummary last) {
        return List.of(
                text(point.getType().getDisplayName()),
                text(point.getName()),
                text(point.getPointNo()),
                crs(point.getTm().crs()),
                decimal(point.getTm().northing()),
                decimal(point.getTm().easting()),
                decimal(BigDecimal.valueOf(point.getGeo().longitude()).setScale(GEO_SCALE, RoundingMode.HALF_UP)),
                decimal(BigDecimal.valueOf(point.getGeo().latitude()).setScale(GEO_SCALE, RoundingMode.HALF_UP)),
                region(point),
                text(point.getAddress()),
                date(point.getInstalledDate()),
                thisRound == null ? "" : thisRound.getResult().getDisplayName(),
                thisRound == null ? "" : date(thisRound.getSurveyedAt().atZoneSameInstant(clock.getZone()).toLocalDate()),
                last == null ? "" : text(last.result()),
                last == null ? "" : date(last.surveyedOn()),
                last == null ? "" : text(last.surveyorName()),
                last == null ? "" : text(last.note()));
    }

    /**
     * 좌표계구분 — 읽는 쪽이 아는 두 어휘로만 적는다.
     *
     * <p>이 열이 가리는 것은 원점이 아니라 측지계다. 원점은 이 서식이 애초에 싣지 않고, 세계측지계 점을
     * 다시 올리면 읽는 쪽이 중부원점으로 되돌린다.
     */
    private static String crs(CoordinateSystem crs) {
        return crs == CoordinateSystem.BESSEL_CENTRAL ? "지역" : "세계";
    }

    /** 토지소재지 — 읽는 쪽이 코드와 이름으로 가르는 그 표기로 되돌린다. */
    private static String region(ControlPoint point) {
        String code = point.getRegionCode();
        String name = point.getRegionName();
        if (code == null || code.isBlank()) {
            return text(name);
        }
        return name == null || name.isBlank() ? code : code + "-" + name;
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static String date(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    /** 꼬리의 0 은 떼고 적는다 — 자릿수를 맞추려 붙인 0 이 성과에 없던 정밀도로 읽힌다. */
    private static String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static String fileName(String projectName) {
        String safe = UNSAFE_FILE_NAME.matcher(projectName).replaceAll(" ").trim();
        return (safe.isEmpty() ? "조사" : safe) + "_기준점.xlsx";
    }

    /**
     * 기준점명 차례 — 글자는 한국어 순, 숫자는 값 순이다("2공"이 "10공" 앞에 온다).
     * 화면 목록이 쓰는 규칙과 같아야 파일과 화면을 나란히 놓고 볼 수 있다.
     */
    private static Comparator<String> byName() {
        return (a, b) -> {
            List<String> left = split(a);
            List<String> right = split(b);
            for (int i = 0; i < Math.min(left.size(), right.size()); i++) {
                String x = left.get(i);
                String y = right.get(i);
                boolean bothNumbers = isNumber(x) && isNumber(y);
                int compared = bothNumbers ? new BigDecimal(x).compareTo(new BigDecimal(y)) : KOREAN.compare(x, y);
                if (compared != 0) {
                    return compared;
                }
            }
            return Integer.compare(left.size(), right.size());
        };
    }

    private static List<String> split(String name) {
        List<String> parts = new ArrayList<>();
        var matcher = DIGITS.matcher(name);
        int at = 0;
        while (matcher.find()) {
            if (matcher.start() > at) {
                parts.add(name.substring(at, matcher.start()));
            }
            parts.add(matcher.group());
            at = matcher.end();
        }
        if (at < name.length()) {
            parts.add(name.substring(at));
        }
        return parts;
    }

    private static boolean isNumber(String part) {
        return !part.isEmpty() && Character.isDigit(part.charAt(0));
    }
}
