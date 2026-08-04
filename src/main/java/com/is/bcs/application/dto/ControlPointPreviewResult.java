package com.is.bcs.application.dto;

import com.is.bcs.domain.controlpoint.PointType;

import java.util.List;

/**
 * 기준점 파일을 등록하지 않고 읽어만 본 결과 — 파일을 어떻게 읽었는지(file)와 점마다 무엇이 벌어지는지(points).
 * 갱신은 기존 성과를 덮으므로 되돌릴 수 없다. 확정 전에 어느 점의 무엇이 바뀌는지 보여 주려고 행마다 판정을 붙인다.
 */
public record ControlPointPreviewResult(ImportPreviewResult file, List<PointPreview> points) {

    /** 이 행이 등록되면 벌어지는 일. */
    public enum Action {
        /** 파일에만 있는 점 — 새로 등록된다. */
        NEW,
        /** 이미 있는 점인데 값이 달라 파일 값으로 덮는다. */
        UPDATE,
        /** 이미 있고 값도 같아 그대로 둔다. */
        UNCHANGED
    }

    /**
     * @param row     원본 파일의 행 번호
     * @param changes 갱신될 항목 — NEW·UNCHANGED 면 비어 있다
     * @param warning 이 행에 대한 경고(부천 범위 밖 등) — 등록을 막지 않고 확인만 요청한다. 없으면 null
     */
    public record PointPreview(
            int row,
            String pointNo,
            PointType type,
            String name,
            String crs,
            String northing,
            String easting,
            String longitude,
            String latitude,
            Action action,
            List<FieldChange> changes,
            String warning
    ) {
    }
}
