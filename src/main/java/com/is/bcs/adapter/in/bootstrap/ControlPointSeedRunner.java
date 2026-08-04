package com.is.bcs.adapter.in.bootstrap;

import com.is.bcs.application.dto.SeedControlPointsCommand;
import com.is.bcs.application.dto.SeedControlPointsCommand.SeedFile;
import com.is.bcs.application.dto.SeedControlPointsResult;
import com.is.bcs.application.port.in.imports.SeedControlPointsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * 기동 시 초기 데이터 자료를 모아 시드 유스케이스에 넘긴다 — 넣을지 판단과 저장은 유스케이스 몫이다.
 * 자료 준비(클래스패스 파싱)는 기동 1회라, 이미 데이터가 있어 버려져도 비용이 무시할 수준이다.
 *
 * 통합 테스트는 각자 자기 데이터를 넣으므로 bcs.seed.enabled=false로 이 러너를 꺼 시드가 개수 단언을 오염시키지 않게 한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "bcs.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ControlPointSeedRunner implements CommandLineRunner {

    /** 고객사가 정리해 준 성과 — 좌표계가 갈려 파일이 둘이다. */
    private static final List<String> CUSTOMER_FILES =
            List.of("/seed/control-points-bucheon-bessel.xlsx", "/seed/control-points-bucheon-grs80.xls");

    private final SeedControlPointsUseCase seedControlPointsUseCase;

    @Override
    public void run(String... args) {
        SeedControlPointsResult result = seedControlPointsUseCase.seedIfEmpty(new SeedControlPointsCommand(
                DogeunSeedCsv.load(),
                CUSTOMER_FILES.stream().map(file -> new SeedFile(file, read(file))).toList()));
        if (!result.seeded()) {
            return;
        }

        for (SeedControlPointsResult.FileSeed file : result.files()) {
            // 건너뛴 행은 원본 파일을 고쳐야 하는 자리다 — 조용히 사라지면 점이 왜 비는지 알 수 없다
            file.result().skipped().forEach(reason -> log.warn("시드에서 건너뛴 행 — {} {}", file.name(), reason));
            file.result().warnings().forEach(reason -> log.warn("시드에 넣었으나 확인이 필요한 행 — {} {}", file.name(), reason));
        }
        log.info("기준점 시드 등록: 도근점 성과 {}점, 고객사 정리분 {}점", result.basePoints(), result.filePoints());
    }

    private static byte[] read(String resource) {
        try (InputStream in = ControlPointSeedRunner.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("시드 파일이 없습니다: " + resource);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
