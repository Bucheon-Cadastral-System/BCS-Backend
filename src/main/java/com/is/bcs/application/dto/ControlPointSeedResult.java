package com.is.bcs.application.dto;

import java.util.List;

/**
 * 시드 등록 결과 — 넣은 점 수, 읽지 못해 건너뛴 행, 넣기는 했으나 값이 의심스러운 행.
 * 두 목록 모두 담당자가 원본 파일을 확인해야 하는 자리이므로 개수만이 아니라 사유를 남긴다.
 */
public record ControlPointSeedResult(int seeded, List<String> skipped, List<String> warnings) {
}
