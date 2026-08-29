package com.is.bcs.adapter.in.ai;

import com.is.bcs.application.port.in.controlpoint.GetControlPointsUseCase;
import com.is.bcs.domain.controlpoint.ControlPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 기준점 챗봇 조회 도구(인바운드 어댑터) — 모델이 도구 호출로 데이터를 묻는 진입점.
 * 조회 유스케이스에 위임만 하고 모델용 표현 변환은 응답 record의 from이 맡는다.
 * 쓰기 유스케이스는 등록하지 않고, 조회 실패 예외는 ChatToolErrorProcessor가 모델용 안내로 바꾼다.
 */
@Component
@RequiredArgsConstructor
public class ControlPointChatTools {

    private final GetControlPointsUseCase getControlPointsUseCase;

    @Tool(description = "부천시 지적기준점의 전체 개수(total)와 종류별 개수(countByType: 지적삼각점·지적삼각보조점·지적도근점, 키는 한글 종류명)를 집계해 반환한다. '기준점 총 몇 개', '도근점 개수' 같은 개수·통계 질문에 사용한다")
    public PointCountSummary countControlPoints() {
        return PointCountSummary.from(getControlPointsUseCase.getCountSummary());
    }

    @Tool(description = """
            이름 · 관리번호 · 소재지로 지적기준점을 찾는다. 사용자가 관리번호를 모를 때 쓰는 길이다.
            keyword는 이름 · 관리번호 · 법정동명 · 상세 소재지 어디에든 걸리면 뽑는다(부분 일치).
            type을 주면 그 종류만 남긴다(지적삼각점 · 지적삼각보조점 · 지적도근점).
            반환: 관리번호pointNo · 이름name · 종류type · 법정동regionName · 소재지address.
            많이 걸리면 앞에서부터 limit 만큼만 준다(기본 10, 최대 30). 좌표까지 필요하면 관리번호로 getControlPointByNo를 부른다.""")
    public List<ControlPointBrief> findControlPoints(
            @ToolParam(description = "찾을 말 — 기준점 이름 · 관리번호 · 법정동 · 소재지") String keyword,
            @ToolParam(required = false, description = "종류 — 지적삼각점 · 지적삼각보조점 · 지적도근점") String type,
            @ToolParam(required = false, description = "최대 건수(기본 10, 최대 30)") Integer limit
    ) {
        String needle = keyword == null ? "" : keyword.trim().toLowerCase(Locale.KOREAN);
        String wantedType = type == null ? null : type.trim();
        return getControlPointsUseCase.getAll().stream()
                .filter(point -> wantedType == null || wantedType.isEmpty()
                        || point.getType().getDisplayName().equals(wantedType))
                .filter(point -> needle.isEmpty() || matches(point, needle))
                .limit(bounded(limit))
                .map(ControlPointBrief::from)
                .toList();
    }

    @Tool(description = "관리번호로 그 기준점의 최종조사를 조회한다. 회차와 무관하게 가장 마지막 조사다. 반환: 결과result · 조사일surveyedOn · 조사원surveyorName · 비고note. 조사한 적이 없으면 결과와 조사일이 비어 있다")
    public LastSurveyBrief getLastSurveyByPointNo(
            @ToolParam(description = "기준점 관리번호 15자리(예: 41192D000001265)") String pointNo) {
        ControlPoint point = getControlPointsUseCase.getByPointNo(pointNo);
        return LastSurveyBrief.from(point, getControlPointsUseCase.getLastSurvey(point.getId()));
    }

    /** 찾는 말이 걸리는 칸 — 어느 칸에 든 값을 기억하는지는 사람마다 달라 한 번에 훑는다. */
    private static boolean matches(ControlPoint point, String needle) {
        return contains(point.getName(), needle)
                || contains(point.getPointNo(), needle)
                || contains(point.getRegionName(), needle)
                || contains(point.getAddress(), needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.KOREAN).contains(needle);
    }

    /** 대화 창에 그릴 수 있는 만큼으로 자른다 — 수백 줄을 넘겨 봐야 모델이 답을 잘라 먹는다. */
    private static int bounded(Integer limit) {
        if (limit == null || limit <= 0) return 10;
        return Math.min(limit, 30);
    }

    @Tool(description = "관리번호(pointNo)로 지적기준점 1점의 상세를 조회한다. 반환: 관리번호·이름·종류·성과좌표계(crs)·TM 성과좌표(northing=북, easting=동)·경위도(longitude·latitude)·소재지(regionName·address)·표석재질·설치구분·설치일자. 특정 기준점 1점의 좌표·소재지 등을 물을 때 사용한다")
    public ControlPointDetail getControlPointByNo(
            @ToolParam(description = "기준점 관리번호 15자리(예: 41192D000001265)") String pointNo) {
        return ControlPointDetail.from(getControlPointsUseCase.getByPointNo(pointNo));
    }
}
