package com.is.bcs.adapter.in.ai;

import com.is.bcs.application.dto.SurveyProgress;
import com.is.bcs.domain.survey.SurveyResult;

/**
 * 조사 진행 현황(모델용).
 *
 * <p>화면(프로젝트 정보 패널)이 쓰는 정상 · 망실 · 조사불가 · 기타 · 미조사 다섯 갈래를 담는다.
 * 다섯은 서로 겹치지 않고 더하면 대상 전체가 된다. 조사한 수는 앞의 넷, 정상 · 망실 · 조사불가 · 기타를 더한 값이다.
 * 결과 이름은 화면이 쓰는 말로 옮겨 담는다. 저장 값의 이름이 달라도 모델은 화면과 같은 말만 쓴다.
 */
public record SurveyProgressSummary(
        String projectName,
        /** 조사 대상 전체 */
        long totalPoints,
        /** 조사한 점. 정상 + 망실 + 조사불가 + 기타 (진행률의 분자) */
        long surveyedPoints,
        /** 정상. 조사한 점에서 망실 · 조사불가 · 기타를 뺀 값 */
        long intactPoints,
        /** 망실 */
        long lostPoints,
        /** 조사불가 */
        long unavailablePoints,
        /** 기타 */
        long etcPoints,
        /** 미조사 */
        long notSurveyedPoints,
        /** 진행률(%) — 화면과 같은 반올림 규칙 */
        int progressPercent
) {

    public static SurveyProgressSummary from(SurveyProgress progress) {
        // 결과별 개수는 서비스가 0으로 채워 넘기지만, 매핑 경계에서도 getOrDefault로 방어한다
        // (long 언박싱이라 키가 없으면 NPE — 부분 맵이 들어와도 0으로 편다)
        long lost = progress.countByResult().getOrDefault(SurveyResult.LOST, 0L);
        long unavailable = progress.countByResult().getOrDefault(SurveyResult.UNAVAILABLE, 0L);
        long etc = progress.countByResult().getOrDefault(SurveyResult.ETC, 0L);
        long surveyed = progress.surveyedPoints();
        return new SurveyProgressSummary(
                progress.projectName(),
                progress.totalPoints(),
                surveyed,
                Math.max(0, surveyed - lost - unavailable - etc),
                lost,
                unavailable,
                etc,
                progress.notSurveyedPoints(),
                percent(surveyed, progress.totalPoints()));
    }

    /** 화면(shared/lib/percent)과 같은 규칙 — 대상이 없으면 0, 그 외에는 반올림. */
    private static int percent(long value, long total) {
        return total == 0 ? 0 : Math.round((float) value / total * 100);
    }
}
