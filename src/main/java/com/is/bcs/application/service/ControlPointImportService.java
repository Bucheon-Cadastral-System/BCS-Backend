package com.is.bcs.application.service;

import com.is.bcs.application.dto.ControlPointImportResult;
import com.is.bcs.application.dto.ControlPointSeedResult;
import com.is.bcs.application.dto.SeedControlPointsCommand;
import com.is.bcs.application.dto.SeedControlPointsResult;
import com.is.bcs.application.port.in.imports.ImportControlPointsUseCase;
import com.is.bcs.application.port.in.imports.SeedControlPointsUseCase;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.application.port.out.file.TableExtractor;
import com.is.bcs.application.service.ImportFileMapper.MappingResult;
import com.is.bcs.application.service.ImportFileMapper.Row;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * 기준점 파일 등록 — 조사를 만들지 않고 기준점 마스터만 반영한다.
 * 파일 서식과 판정 규칙은 대상지 임포트와 같으므로 같은 매퍼·같은 반영기를 쓴다.
 *
 * 파일 파싱은 DB 를 건드리지 않으므로 트랜잭션 밖에서 끝낸다 — 수천 행을 읽는 동안 커넥션을 잡고 있으면
 * 동시에 다른 담당자가 올릴 때 풀이 마른다. 트랜잭션은 실제로 쓰는 구간(store)에만 건다.
 */
@Service
@RequiredArgsConstructor
public class ControlPointImportService implements ImportControlPointsUseCase, SeedControlPointsUseCase {

    private final TableExtractor tableExtractor;
    private final ControlPointFileMapper controlPointFileMapper;
    private final ControlPointRegistrar controlPointRegistrar;
    private final TransactionTemplate transactionTemplate;
    private final LoadControlPointPort loadControlPointPort;
    private final SaveControlPointPort saveControlPointPort;

    @Override
    public ControlPointImportResult importControlPoints(byte[] content) {
        MappingResult mapped = controlPointFileMapper.map(tableExtractor.extract(content));
        mapped.rejectIfAnyRowFailed();

        return transactionTemplate.execute(status -> store(mapped.rows()));
    }

    @Override
    public SeedControlPointsResult seedIfEmpty(SeedControlPointsCommand command) {
        if (loadControlPointPort.count() > 0) {
            return SeedControlPointsResult.skipped();
        }
        // 성과 점을 먼저, 파일을 나중에 — 겹치는 점은 파일 값이 남는다
        int basePoints = transactionTemplate.execute(status -> saveControlPointPort.saveAll(command.points())).size();
        List<SeedControlPointsResult.FileSeed> files = command.files().stream()
                .map(file -> new SeedControlPointsResult.FileSeed(file.name(), seed(file.content())))
                .toList();
        return new SeedControlPointsResult(true, basePoints, files);
    }

    /** 시드는 읽히는 행만 넣는다 — 담당자가 올린 파일이 아니라 함께 배포되는 파일이라, 고칠 사람이 그 자리에 없다. */
    private ControlPointSeedResult seed(byte[] content) {
        MappingResult mapped = controlPointFileMapper.map(tableExtractor.extract(content));
        ControlPointImportResult stored = transactionTemplate.execute(status -> store(mapped.rows()));

        return new ControlPointSeedResult(
                stored.newPoints() + stored.updatedPoints(),
                mapped.errors().stream().map(e -> e.row() + "행: " + e.message()).toList(),
                mapped.warnings().stream().map(w -> w.row() + "행: " + w.message()).toList());
    }

    private ControlPointImportResult store(List<Row> rows) {
        ControlPointRegistrar.Result points = controlPointRegistrar.register(rows);
        return new ControlPointImportResult(
                rows.size(), points.newPoints(), points.existingPoints(), points.updatedPoints());
    }
}
