package com.is.bcs.adapter.in.web.controlpoint;

import com.is.bcs.application.dto.PointLastSurvey;
import com.is.bcs.domain.survey.SurveyResult;

import java.time.LocalDate;

/**
 * 지도가 점마다 색을 고를 때 읽는 최종조사 한 줄.
 *
 * <p>{@link LastSurveyResponse} 와 달리 결과를 어휘로 내린다. 저쪽은 상세 카드가 사람에게 그대로 보여 주는
 * 문구라 시드의 자유 표기를 살리지만, 이쪽은 화면이 색으로 옮기므로 아는 말 넷 중 하나여야 한다.
 *
 * @param pointId    기준점 id
 * @param result     조사 결과
 * @param surveyedOn 조사일
 */
public record PointLastSurveyResponse(Long pointId, SurveyResult result, LocalDate surveyedOn) {

    public static PointLastSurveyResponse from(PointLastSurvey lastSurvey) {
        return new PointLastSurveyResponse(lastSurvey.pointId(), lastSurvey.result(), lastSurvey.surveyedOn());
    }
}
