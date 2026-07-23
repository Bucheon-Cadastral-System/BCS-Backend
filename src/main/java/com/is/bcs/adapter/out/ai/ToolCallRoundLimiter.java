package com.is.bcs.adapter.out.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.tool.ToolExecutionEligibilityChecker;
import org.springframework.stereotype.Component;

/**
 * 도구 호출 라운드 상한 — 광범위한 질의나 도구 실패 반복으로 에이전트 루프가 상한 없이 길어지면
 * 지연·비용이 커지는데 Spring AI에 빌트인 상한이 없어 체커에 카운터를 둔다.
 * 상한을 넘으면 도구를 더 실행하지 않고 그 시점의 모델 응답으로 마무리한다
 * (빈 답변은 ChatModelAdapter가 안내 문구로 대체).
 * 카운터는 ThreadLocal — 요청마다 스레드가 새로 뜨는 가상 스레드 구성이지만,
 * 루프가 끝나는 모든 분기에서 지워 스레드 재사용에도 안전하게 한다.
 */
@Slf4j
@Component
public class ToolCallRoundLimiter implements ToolExecutionEligibilityChecker {

    /** 질문 하나에 필요한 도구 호출은 보통 2~3라운드 — 여유를 둔 상한. */
    static final int MAX_ROUNDS = 5;

    private final ThreadLocal<Integer> rounds = ThreadLocal.withInitial(() -> 0);

    @Override
    public Boolean apply(ChatResponse chatResponse) {
        if (chatResponse == null || !chatResponse.hasToolCalls()) {
            rounds.remove(); // 루프 정상 종료 — 카운터 정리
            return false;
        }
        int round = rounds.get() + 1;
        if (round > MAX_ROUNDS) {
            rounds.remove();
            log.warn("도구 호출 라운드 상한({}) 초과 — 에이전트 루프를 중단한다", MAX_ROUNDS);
            return false;
        }
        rounds.set(round);
        return true;
    }
}
