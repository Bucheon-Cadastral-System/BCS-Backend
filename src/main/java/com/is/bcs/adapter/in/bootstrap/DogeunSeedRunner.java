package com.is.bcs.adapter.in.bootstrap;

import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** 기동 시 기준점이 하나도 없으면 부천 도근점 시드를 등록한다 — 기존 데이터가 있으면 손대지 않는다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DogeunSeedRunner implements CommandLineRunner {

    private final LoadControlPointPort loadControlPointPort;
    private final SaveControlPointPort saveControlPointPort;

    @Override
    public void run(String... args) {
        if (loadControlPointPort.count() > 0) {
            return;
        }

        int seeded = saveControlPointPort.saveAll(DogeunSeedCsv.load()).size();
        log.info("부천 도근점 시드 등록: {}점", seeded);
    }
}
