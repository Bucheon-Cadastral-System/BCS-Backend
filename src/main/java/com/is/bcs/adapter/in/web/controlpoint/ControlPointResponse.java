package com.is.bcs.adapter.in.web.controlpoint;

import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.PointType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 화면에 세우는 기준점 한 점.
 *
 * <p>도메인이 들고 있는 값을 다 싣지는 않는다. 소재지·상세주소·표지재질·설치구분·도선정보는 파일이 들고 와
 * 저장돼 있지만 그리는 자리가 없다. 응답에 남겨 두면 화면이 쓰지 않는 값을 점 수천 개만큼 나르고,
 * 언젠가 누군가 그 값에 기대기 시작하면 뺄 때 양쪽을 함께 고쳐야 한다. 그릴 자리가 생기면 그때 싣는다.
 *
 * <p>최종조사 요약(결과·조사일·조사원)도 같은 이유로 싣지 않는다. 점 하나를 고른 뒤에만 보이는 값이라
 * 목록에 실으면 역시 수천 행만큼 헛돈다.
 */
public record ControlPointResponse(
        Long id,
        String pointNo,
        PointType type,
        String name,
        CoordinateSystem crs,
        BigDecimal northing,
        BigDecimal easting,
        double longitude,
        double latitude,
        LocalDate installedDate,
        /** 판 번호 — 수정 요청이 그대로 돌려보내 그사이 다른 사람이 먼저 고쳤는지 가린다 */
        long version
) {

    public static ControlPointResponse from(ControlPoint point) {
        return new ControlPointResponse(
                point.getId(), point.getPointNo(), point.getType(), point.getName(),
                point.getTm().crs(), point.getTm().northing(), point.getTm().easting(),
                point.getGeo().longitude(), point.getGeo().latitude(),
                point.getInstalledDate(), point.getVersion()
        );
    }
}
