package com.is.bcs.application.dto;

import java.time.LocalDate;

/**
 * 기준점의 최종조사 요약 — 회차와 무관하게 가장 마지막으로 조사한 결과다.
 *
 * <p>세 값은 늘 함께 읽히고 함께 바뀐다. 조사 기록을 남기거나 지우면 서버가 셋을 한꺼번에 다시 계산한다.
 * 목록에는 싣지 않는다. 점 하나를 고른 뒤에만 필요한 값이라 그때 따로 읽는다.
 *
 * @param result      최종조사내용. 파일로 들어온 값도 화면 어휘로 맞춰 둔다
 * @param surveyedOn  최종조사일
 * @param surveyorName 최종조사원 표시명. 시드 조사와 인증 전에 남긴 기록은 비어 있다
 * @param note        판정에 딸린 비고. 기타가 아니거나 시드 조사면 비어 있다
 */
public record LastSurveySummary(String result, LocalDate surveyedOn, String surveyorName, String note) {
}
