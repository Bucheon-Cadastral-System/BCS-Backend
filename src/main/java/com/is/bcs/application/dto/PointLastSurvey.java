package com.is.bcs.application.dto;

import com.is.bcs.domain.survey.SurveyResult;

import java.time.LocalDate;

/**
 * 기준점 하나의 최종조사 — 지도가 점마다 색을 고르는 데 필요한 최소 묶음.
 *
 * <p>{@link LastSurveySummary} 와 뜻은 같고 자리가 다르다. 저쪽은 점 하나를 고른 뒤 상세 카드가 읽는 값이라
 * 조사원과 비고까지 싣고 결과를 사람이 읽는 문구로 내린다. 이쪽은 수천 점을 한 번에 받아 색으로만 옮기므로
 * 조사원과 비고를 빼고 결과를 어휘로 고정한다.
 *
 * @param pointId    기준점 id
 * @param result     조사 결과. 시드의 자유 표기도 여기서는 어휘 안으로 맞춘다
 * @param surveyedOn 조사일. 시드와 조사기록 중 어느 쪽을 골랐는지 가른 값이다
 */
public record PointLastSurvey(Long pointId, SurveyResult result, LocalDate surveyedOn) {
}
