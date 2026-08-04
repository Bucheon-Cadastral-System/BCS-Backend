package com.is.bcs.application.service;

import com.is.bcs.application.dto.ControlPointPreviewResult;
import com.is.bcs.application.dto.ControlPointPreviewResult.Action;
import com.is.bcs.application.dto.ControlPointPreviewResult.PointPreview;
import com.is.bcs.application.dto.ImportPreviewResult;
import com.is.bcs.application.port.in.imports.PreviewControlPointsUseCase;
import com.is.bcs.application.port.in.imports.PreviewSurveyCsvUseCase;
import com.is.bcs.application.port.out.file.TableExtractor;
import com.is.bcs.application.service.ImportFileMapper.Row;
import com.is.bcs.domain.controlpoint.ControlPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 대상지 파일 미리보기 — 읽기만 하므로 저장소를 건드리지 않는다.
 * 확정(임포트)할 때 파일을 다시 보내는 대신 중간 상태를 서버에 두지 않는 쪽을 택했다.
 */
@Service
@RequiredArgsConstructor
public class SurveyCsvPreviewService implements PreviewSurveyCsvUseCase, PreviewControlPointsUseCase {

    private final TableExtractor tableExtractor;
    private final SurveyTargetMapper surveyTargetMapper;
    private final ControlPointFileMapper controlPointFileMapper;
    private final ControlPointRegistrar controlPointRegistrar;

    @Override
    public ImportPreviewResult preview(byte[] content) {
        return toResult(surveyTargetMapper.map(tableExtractor.extract(content)));
    }

    /**
     * 기준점 미리보기는 저장소를 읽는다 — 어느 점이 새로 생기고 어느 점의 무엇이 덮이는지는 기존 값과 대조해야 알 수 있다.
     * 쓰지는 않으므로 확정 전에 바뀌는 것은 없고, 판정은 등록이 쓰는 규칙을 그대로 쓴다.
     */
    @Override
    public ControlPointPreviewResult previewControlPoints(byte[] content) {
        ImportFileMapper.MappingResult mapped = controlPointFileMapper.map(tableExtractor.extract(content));
        ControlPointRegistrar.Candidates candidates = controlPointRegistrar.candidates(mapped.rows());
        // 부천 범위 밖 같은 행 경고는 그 점의 줄에 붙인다 — 목록과 따로 놀면 어느 점 이야기인지 찾아야 한다
        Map<Integer, String> warningByRow = mapped.warnings().stream().collect(Collectors.toMap(
                ImportFileMapper.RowWarning::row, ImportFileMapper.RowWarning::message, (a, b) -> a + " / " + b));

        List<PointPreview> points = new ArrayList<>(mapped.rows().size());
        List<ImportFileMapper.RowError> conflicts = new ArrayList<>();
        for (Row row : mapped.rows()) {
            ControlPoint matched = candidates.match(row);
            // 등록이 거부할 관리번호 충돌을 같은 판정으로 미리 걸러 행 오류로 보여 준다 — 등록을 눌러야 아는 실패를 없앤다
            String taken = ControlPointRegistrar.pointNoTakenBy(row, matched, candidates.pointNoOwner(row));
            if (taken != null) {
                conflicts.add(new ImportFileMapper.RowError(row.sourceRow(), taken));
            } else {
                points.add(toPreview(row, matched, warningByRow.get(row.sourceRow())));
            }
        }
        return new ControlPointPreviewResult(toResult(mapped, conflicts), List.copyOf(points));
    }

    private static PointPreview toPreview(Row row, ControlPoint found, String warning) {
        List<ControlPointRegistrar.FieldChange> changes =
                found == null ? List.of() : ControlPointRegistrar.changes(found, row);
        Action action = found == null ? Action.NEW : changes.isEmpty() ? Action.UNCHANGED : Action.UPDATE;

        return new PointPreview(
                row.sourceRow(), row.pointNo(), row.type(), row.name(),
                row.tm().crs().getDisplayName(),
                row.tm().northing().toPlainString(), row.tm().easting().toPlainString(),
                "%.6f".formatted(row.geo().longitude()), "%.6f".formatted(row.geo().latitude()),
                action, changes, warning);
    }

    private static ImportPreviewResult toResult(ImportFileMapper.MappingResult mapped) {
        return toResult(mapped, List.of());
    }

    /** 매퍼가 모은 행 오류에 미리보기 단계에서 판정한 오류(관리번호 충돌)를 행 순서로 합친다. */
    private static ImportPreviewResult toResult(
            ImportFileMapper.MappingResult mapped, List<ImportFileMapper.RowError> extraErrors) {
        return new ImportPreviewResult(
                mapped.totalRows(),
                mapped.columns().recognized(),
                mapped.columns().extra(),
                Stream.concat(mapped.errors().stream(), extraErrors.stream())
                        .sorted(Comparator.comparingInt(ImportFileMapper.RowError::row))
                        .map(e -> new ImportPreviewResult.RowError(e.row(), e.message()))
                        .toList(),
                mapped.warnings().stream()
                        .map(w -> new ImportPreviewResult.RowWarning(w.row(), w.message()))
                        .toList(),
                mapped.missingColumns(),
                mapped.foreignColumns());
    }
}
