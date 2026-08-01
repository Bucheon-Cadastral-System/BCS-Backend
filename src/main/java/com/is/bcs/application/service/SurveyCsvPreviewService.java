package com.is.bcs.application.service;

import com.is.bcs.application.dto.SurveyCsvPreviewResult;
import com.is.bcs.application.port.in.imports.PreviewSurveyCsvUseCase;
import com.is.bcs.application.port.out.file.TableExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 대상지 파일 미리보기 — 읽기만 하므로 저장소를 건드리지 않는다.
 * 확정(임포트)할 때 파일을 다시 보내는 대신 중간 상태를 서버에 두지 않는 쪽을 택했다.
 */
@Service
@RequiredArgsConstructor
public class SurveyCsvPreviewService implements PreviewSurveyCsvUseCase {

    private final TableExtractor tableExtractor;

    @Override
    public SurveyCsvPreviewResult preview(byte[] content) {
        SurveyTargetMapper.MappingResult mapped = SurveyTargetMapper.map(tableExtractor.extract(content));

        return new SurveyCsvPreviewResult(
                mapped.totalRows(),
                mapped.columns().recognized(),
                mapped.columns().extra(),
                mapped.errors().stream()
                        .map(e -> new SurveyCsvPreviewResult.RowError(e.row(), e.message()))
                        .toList());
    }
}
