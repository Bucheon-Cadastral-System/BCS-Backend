package com.is.bcs.application.dto;

import com.is.bcs.domain.controlpoint.ControlPoint;

/**
 * 기준점 수동 등록 결과 — 임포트와 같은 규칙이라 신규 등록만이 아니라 기존 점 갱신·재사용으로도 끝난다.
 * 어느 쪽이었는지와, 등록은 됐지만 확인이 필요한 경고를 함께 돌려준다.
 *
 * @param created 파일에 없던 새 점을 만들었는지
 * @param updated 같은 이름·종류의 기존 점을 입력 값으로 덮었는지 (created·updated 둘 다 아니면 값이 같아 그대로 둔 것)
 * @param warning 부천 범위 밖 좌표 등 확인 요청 — 등록을 막지 않는다. 없으면 null
 */
public record RegisterControlPointResult(ControlPoint point, boolean created, boolean updated, String warning) {
}
