package com.is.bcs.application.dto;

import com.is.bcs.domain.controlpoint.ControlPoint;

import java.util.List;

/**
 * 시드 자료 — 완성된 성과 점과, 등록 경로로 통과시킬 파일들.
 *
 * @param points 좌표 변환까지 끝난 점(도근점 성과) — 매칭 없이 그대로 저장된다
 * @param files  고객사 정리 파일 — 담당자가 올리는 것과 같은 경로로 읽는다
 */
public record SeedControlPointsCommand(List<ControlPoint> points, List<SeedFile> files) {

    public record SeedFile(String name, byte[] content) {
    }
}
