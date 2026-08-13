package com.is.bcs.adapter.in.bootstrap;

import com.is.bcs.application.dto.SeedControlPointsCommand;
import com.is.bcs.application.dto.SeedControlPointsCommand.SeedFile;
import com.is.bcs.application.dto.SeedControlPointsResult;
import com.is.bcs.application.port.in.imports.SeedControlPointsUseCase;
import com.is.bcs.domain.controlpoint.ControlPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class ControlPointSeedRunner implements CommandLineRunner {

    /** 고객사가 정리해 준 성과 — 좌표계가 갈려 파일이 둘이다. */
    private static final List<String> CUSTOMER_FILES =
            List.of("/seed/control-points-bucheon-bessel.xlsx", "/seed/control-points-bucheon-grs80.xls");

    private final SeedControlPointsUseCase seedControlPointsUseCase;

    /**
     * 도근점 성과(CSV) 를 넣을지 — 기본은 넣지 않는다.
     *
     * <p>고객사가 이 자료를 쓰지 않기로 해 껐다. 파일과 로더는 그대로 두어 다시 필요해지면 값만 켜면 된다.
     * 이미 들어가 있는 점을 지우지는 않는다. 시드는 기준점이 하나도 없을 때만 도는 경로다.
     */
    private final boolean dogeunEnabled;

    public ControlPointSeedRunner(
            SeedControlPointsUseCase seedControlPointsUseCase,
            @Value("${bcs.seed.dogeun.enabled:false}") boolean dogeunEnabled
    ) {
        this.seedControlPointsUseCase = seedControlPointsUseCase;
        this.dogeunEnabled = dogeunEnabled;
    }

    @Override
    public void run(String... args) {
        List<ControlPoint> basePoints = dogeunEnabled ? DogeunSeedCsv.load() : List.of();
        SeedControlPointsResult result = seedControlPointsUseCase.seedIfEmpty(new SeedControlPointsCommand(
                basePoints,
                CUSTOMER_FILES.stream().map(file -> new SeedFile(file, read(file))).toList()));
        if (!result.seeded()) {
            return;
        }

        for (SeedControlPointsResult.FileSeed file : result.files()) {
            // 건너뛴 행은 원본 파일을 고쳐야 하는 자리다 — 조용히 사라지면 점이 왜 비는지 알 수 없다
            file.result().skipped().forEach(reason -> log.warn("시드에서 건너뛴 행 — {} {}", file.name(), reason));
            file.result().warnings().forEach(reason -> log.warn("시드에 넣었으나 확인이 필요한 행 — {} {}", file.name(), reason));
        }
        log.info("기준점 시드 등록: 도근점 성과 {}, 고객사 정리분 {}점",
                dogeunEnabled ? result.basePoints() + "점" : "꺼짐", result.filePoints());
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
