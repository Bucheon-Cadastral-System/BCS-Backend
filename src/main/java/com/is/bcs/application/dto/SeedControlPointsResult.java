package com.is.bcs.application.dto;

import java.util.List;

/**
 * 시드 실행 결과 — 이미 데이터가 있어 건너뛰었는지, 넣었다면 무엇을 넣었는지.
 * 파일별 결과를 이름과 함께 남겨, 확인이 필요한 행이 어느 파일 이야기인지 알 수 있게 한다.
 */
public record SeedControlPointsResult(boolean seeded, int basePoints, List<FileSeed> files) {

    public record FileSeed(String name, ControlPointSeedResult result) {
    }

    /** 기준점이 이미 있어 아무것도 하지 않았다. */
    public static SeedControlPointsResult skipped() {
        return new SeedControlPointsResult(false, 0, List.of());
    }

    public int filePoints() {
        return files.stream().mapToInt(file -> file.result().seeded()).sum();
    }
}
