package com.is.bcs.domain.controlpoint;

/**
 * 도선 정보(도선등급·도선명·도호·교차점 여부) — 도근점에만 있는 측량 계보 속성이라 별도 값으로 묶는다.
 * 교차점 = 도선망에서 둘 이상의 도선이 만나는 지점의 점(그 외 일반 도근점).
 * 각 항목은 원천 데이터에 결측이 흔해 개별 null을 허용한다(intersection null = 미기재).
 */
public record TraverseInfo(String grade, String lineName, String lineNo, Boolean intersection) {
}
