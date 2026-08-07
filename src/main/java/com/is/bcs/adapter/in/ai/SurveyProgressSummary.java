package com.is.bcs.adapter.in.ai;

import com.is.bcs.application.dto.SurveyProgress;
import com.is.bcs.domain.survey.SurveyResult;

/**
 * 조사 진행 현황(모델용).
 *
 * <p>화면(프로젝트 정보 패널)이 쓰는 정상 · 망실 · 미조사 세 갈래만 담는다.
 * 셋은 서로 겹치지 않고 더하면 대상 전체가 된다. 조사한 수는 정상과 망실의 합이다.
 * 저장된 결과 이름(완전 · 조사불가 · 기타)은 화면에 나오지 않으므로 모델에도 넘기지 않는다.
 * 모델은 받은 값만 말할 수 있으므로, 화면에 없는 어휘는 여기서 잘라 두는 편이 프롬프트로 막는 것보다 확실하다.
 */
public record SurveyProgressSummary(
        String projectName,
        /** 조사 대상 전체 */
        long totalPoints,
        /** 조사한 점 = 정상 + 망실 (진행률의 분자) */
        long surveyedPoints,
        /** 정상 = 조사한 점 중 망실이 아닌 것 */
        long intactPoints,
        /** 망실 */
        long lostPoints,
        /** 미조사 */
        long notSurveyedPoints,
        /** 진행률(%) — 화면과 같은 반올림 규칙 */
        int progressPercent
) {

    public static SurveyProgressSummary from(SurveyProgress progress) {
        // 결과별 개수는 서비스가 0으로 채워 넘기지만, 매핑 경계에서도 getOrDefault로 방어한다
        // (long 언박싱이라 키가 없으면 NPE — 부분 맵이 들어와도 0으로 편다)
        long lost = progress.countByResult().getOrDefault(SurveyResult.LOST, 0L);
        long surveyed = progress.surveyedPoints();
        return new SurveyProgressSummary(
                progress.projectName(),
                progress.totalPoints(),
                surveyed,
                Math.max(0, surveyed - lost),
                lost,
                progress.notSurveyedPoints(),
                percent(surveyed, progress.totalPoints()));
    }

    /** 화면(shared/lib/percent)과 같은 규칙 — 대상이 없으면 0, 그 외에는 반올림. */
    private static int percent(long value, long total) {
        return total == 0 ? 0 : Math.round((float) value / total * 100);
    }
}
