package com.is.bcs.adapter.in.bootstrap;

import com.is.bcs.application.dto.ControlPointSeedResult;
import com.is.bcs.application.port.in.imports.SeedControlPointsUseCase;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
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
 * 기동 시 기준점이 하나도 없으면 초기 데이터를 등록한다 — 기존 데이터가 있으면 손대지 않는다.
 * 지적도근점 성과에서 뽑은 점을 먼저 넣고, 고객사가 정리해 준 점을 나중에 넣어 겹치는 점은 고객사 값이 남게 한다.
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

    private final LoadControlPointPort loadControlPointPort;
    private final SaveControlPointPort saveControlPointPort;
    private final SeedControlPointsUseCase seedControlPointsUseCase;

    @Override
    public void run(String... args) {
        if (loadControlPointPort.count() > 0) {
            return;
        }

        int dogeun = saveControlPointPort.saveAll(DogeunSeedCsv.load()).size();
        int customer = 0;
        for (String file : CUSTOMER_FILES) {
            ControlPointSeedResult result = seedControlPointsUseCase.seed(read(file));
            customer += result.seeded();
            // 건너뛴 행은 원본 파일을 고쳐야 하는 자리다 — 조용히 사라지면 점이 왜 비는지 알 수 없다
            result.skipped().forEach(reason -> log.warn("시드에서 건너뛴 행 — {} {}", file, reason));
            result.warnings().forEach(reason -> log.warn("시드에 넣었으나 확인이 필요한 행 — {} {}", file, reason));
        }

        log.info("기준점 시드 등록: 도근점 성과 {}점, 고객사 정리분 {}점", dogeun, customer);
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
