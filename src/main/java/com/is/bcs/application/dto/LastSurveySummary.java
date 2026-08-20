package com.is.bcs.application.dto;

import com.is.bcs.domain.survey.SurveyRecord;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 기준점의 최종조사 요약 — 회차와 무관하게 가장 마지막으로 조사한 결과다.
 *
 * <p>세 값은 늘 함께 읽히고 함께 바뀐다. 조사 기록을 남기거나 지우면 서버가 셋을 한꺼번에 다시 계산한다.
 * 목록에는 싣지 않는다. 점 하나를 고른 뒤에만 필요한 값이라 그때 따로 읽는다.
 *
 * @param result      최종조사내용. 파일로 들어온 값도 화면 어휘로 맞춰 둔다
 * @param surveyedOn  최종조사일
 * @param surveyorId  최종조사원 회원 id. 화면이 그 사람의 신원을 물을 때 쓴다
 * @param surveyorName 최종조사원 표시명. 시드 조사와 인증 전에 남긴 기록은 비어 있다
 * @param note        판정에 딸린 비고. 기타가 아니거나 시드 조사면 비어 있다
 */
public record LastSurveySummary(
        String result, LocalDate surveyedOn, Long surveyorId, String surveyorName, String note) {

    /** 조사기록 한 줄을 요약으로 — 조사 시각을 조사일로 내리는 시간대는 부르는 쪽이 정한다. */
    public static LastSurveySummary of(SurveyRecord record, String surveyorName, ZoneId zone) {
        return new LastSurveySummary(
                record.getResult().getDisplayName(),
                record.getSurveyedAt().atZoneSameInstant(zone).toLocalDate(),
                record.getSurveyedById(),
                surveyorName,
                record.getNote());
    }

    /**
     * 시드 조사와 기록 중 최종조사로 삼을 쪽 — 날짜가 늦은 쪽이다.
     *
     * <p>기록이 있다고 무조건 택하지 않는다. 대상지 파일 임포트가 기존조사일로 과거 날짜 기록을 만들기 때문에
     * 시드보다 오래된 기록이 실재한다. 날짜가 같으면 기록을 택한다 — 조사원까지 아는 쪽이 더 자세하다.
     * 한쪽 날짜가 비면 있는 쪽이 이긴다.
     */
    public static LastSurveySummary later(LastSurveySummary seed, LastSurveySummary record) {
        if (record == null) {
            return seed;
        }
        if (seed == null || seed.surveyedOn() == null) {
            return record;
        }
        return record.surveyedOn() != null && !record.surveyedOn().isBefore(seed.surveyedOn()) ? record : seed;
    }
}
