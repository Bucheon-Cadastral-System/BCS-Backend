package com.is.bcs.adapter.in.ai;

import com.is.bcs.application.dto.SurveyProgress;
import com.is.bcs.domain.survey.SurveyResult;

/**
 * 조사 진행 현황(모델용).
 *
 * <p>필드 이름과 묶음을 화면(프로젝트 정보 패널)과 같은 축으로 맞춘다.
 * 화면은 조사 상태를 조사완료 · 망실 · 미조사로 나누고, 조사 결과(완전 · 조사불가 · 기타)는 그 아래 세부로 둔다.
 * 모델이 뺄셈으로 두 축을 스스로 만들면 화면과 다른 수치가 나오므로, 두 축을 모두 계산해 내려보낸다.
 */
public record SurveyProgressSummary(
        String projectName,
        /** 조사 대상 전체 */
        long totalPoints,
        /** 조사한 점 = 조사완료 + 망실 (진행률의 분자) */
        long surveyedPoints,
        /** 조사완료 = 조사한 점 중 망실이 아닌 것 (완전 + 조사불가 + 기타) */
        long completedPoints,
        /** 망실 */
        long lostPoints,
        /** 미조사 */
        long notSurveyedPoints,
        /** 진행률(%) — 화면과 같은 반올림 규칙 */
        int progressPercent,
        /** 조사 결과 세부: 완전 */
        long resultIntactPoints,
        /** 조사 결과 세부: 조사불가 */
        long resultUnavailablePoints,
        /** 조사 결과 세부: 기타 */
        long resultEtcPoints
) {

    public static SurveyProgressSummary from(SurveyProgress progress) {
        // 결과별 개수는 서비스가 0으로 채워 넘기지만, 매핑 경계에서도 getOrDefault로 방어한다
        // (long 언박싱이라 키가 없으면 NPE — 부분 맵이 들어와도 0으로 편다)
        long intact = progress.countByResult().getOrDefault(SurveyResult.INTACT, 0L);
        long lost = progress.countByResult().getOrDefault(SurveyResult.LOST, 0L);
        long unavailable = progress.countByResult().getOrDefault(SurveyResult.UNAVAILABLE, 0L);
        long etc = progress.countByResult().getOrDefault(SurveyResult.ETC, 0L);
        long surveyed = progress.surveyedPoints();
        return new SurveyProgressSummary(
                progress.projectName(),
                progress.totalPoints(),
                surveyed,
                Math.max(0, surveyed - lost),
                lost,
                progress.notSurveyedPoints(),
                percent(surveyed, progress.totalPoints()),
                intact,
                unavailable,
                etc);
    }

    /** 화면(shared/lib/percent)과 같은 규칙 — 대상이 없으면 0, 그 외에는 반올림. */
    private static int percent(long value, long total) {
        return total == 0 ? 0 : Math.round((float) value / total * 100);
    }
}
