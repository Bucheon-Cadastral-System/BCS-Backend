package com.is.bcs.adapter.in.ai;

import com.is.bcs.application.dto.ControlPointCountSummary;
import com.is.bcs.application.port.in.controlpoint.GetControlPointsUseCase;
import com.is.bcs.domain.controlpoint.ControlPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 기준점 챗봇 조회 도구(인바운드 어댑터) — 모델이 도구 호출로 데이터를 묻는 진입점.
 * 조회 유스케이스에 위임하고 표시용 한글 값으로 매핑만 한다. 쓰기 유스케이스는 등록하지 않는다.
 * 조회 실패 예외는 여기서 잡지 않는다 — ChatToolErrorProcessor가 모델용 안내로 바꾼다.
 */
@Component
@RequiredArgsConstructor
public class ControlPointChatTools {

    private final GetControlPointsUseCase getControlPointsUseCase;

    /** 종류별 개수 요약 — countByType 키는 종류의 한글 표시명. */
    public record PointCountSummary(long total, Map<String, Long> countByType) {
    }

    public record ControlPointDetail(
            String pointNo, String name, String type, String crs,
            BigDecimal northing, BigDecimal easting,
            double longitude, double latitude,
            String regionName, String address,
            String markerMaterial, String installType, LocalDate installedDate
    ) {
    }

    @Tool(description = "지적기준점 전체 개수와 종류별(지적삼각점·지적삼각보조점·지적도근점) 개수를 조회한다.")
    public PointCountSummary countControlPoints() {
        ControlPointCountSummary summary = getControlPointsUseCase.getCountSummary();
        Map<String, Long> countByType = new LinkedHashMap<>();
        summary.countByType().forEach((type, count) -> countByType.put(type.getDisplayName(), count));
        return new PointCountSummary(summary.total(), countByType);
    }

    @Tool(description = "관리번호로 지적기준점 1점의 상세(이름·종류·성과좌표·경위도·소재지·설치 정보)를 조회한다.")
    public ControlPointDetail getControlPointByNo(
            @ToolParam(description = "기준점 관리번호(예: 41192D000001265)") String pointNo) {
        ControlPoint point = getControlPointsUseCase.getByPointNo(pointNo);
        return new ControlPointDetail(
                point.getPointNo(), point.getName(), point.getType().getDisplayName(),
                point.getTm().crs().getDisplayName(),
                point.getTm().northing(), point.getTm().easting(),
                point.getGeo().longitude(), point.getGeo().latitude(),
                point.getRegionName(), point.getAddress(),
                point.getMarkerMaterial() == null ? null : point.getMarkerMaterial().getDisplayName(),
                point.getInstallType() == null ? null : point.getInstallType().getDisplayName(),
                point.getInstalledDate());
    }
}
