package com.is.bcs.adapter.in.web.imports;

import com.is.bcs.application.dto.ControlPointPreviewResult;

import java.util.List;
import java.util.Map;

/**
 * 기준점 파일 미리보기 응답 — 파일을 어떻게 읽었는지와, 점마다 등록하면 무엇이 벌어지는지.
 *
 * @param points 파일에 적힌 순서 그대로. 갱신되는 점은 바뀔 항목을 함께 싣는다.
 */
public record ControlPointPreviewResponse(
        int totalRows,
        List<ImportPreviewResponse.RowError> errors,
        List<ImportPreviewResponse.RowWarning> warnings,
        List<String> missingColumns,
        List<String> foreignColumns,
        List<PointPreview> points
) {

    /** 이 행이 등록되면 벌어지는 일 — NEW 새로 등록, UPDATE 기존 값을 덮음, UNCHANGED 그대로 둠. */
    public record PointPreview(
            int row,
            String pointNo,
            String name,
            String crs,
            String northing,
            String easting,
            String action,
            List<FieldChange> changes,
            /** 이 행에 대한 경고(부천 범위 밖 등) — 등록을 막지 않는다. 없으면 null */
            String warning
    ) {
    }

    /** 갱신되는 항목 하나 — 무엇이 어떤 값에서 어떤 값으로 바뀌는지. */
    public record FieldChange(String field, String before, String after) {
    }

    public static ControlPointPreviewResponse from(ControlPointPreviewResult result) {
        ImportPreviewResponse file = ImportPreviewResponse.from(result.file());

        return new ControlPointPreviewResponse(
                file.totalRows(),
                file.errors(), file.warnings(), file.missingColumns(), file.foreignColumns(),
                result.points().stream().map(ControlPointPreviewResponse::toPoint).toList());
    }

    private static PointPreview toPoint(ControlPointPreviewResult.PointPreview point) {
        return new PointPreview(
                point.row(), point.pointNo(), point.name(),
                point.crs(), point.northing(), point.easting(),
                point.action().name(),
                point.changes().stream()
                        .map(c -> new FieldChange(c.field(), c.before(), c.after()))
                        .toList(),
                point.warning());
    }
}
