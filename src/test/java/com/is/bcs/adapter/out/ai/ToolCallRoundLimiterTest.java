package com.is.bcs.adapter.out.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCallRoundLimiterTest {

    private final ToolCallRoundLimiter limiter = new ToolCallRoundLimiter();

    private static ChatResponse withToolCalls() {
        AssistantMessage message = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("1", "function", "countControlPoints", "{}")))
                .build();
        return new ChatResponse(List.of(new Generation(message)));
    }

    private static ChatResponse withoutToolCalls() {
        return new ChatResponse(List.of(new Generation(new AssistantMessage("답변"))));
    }

    @Test
    @DisplayName("도구 호출이 없으면 루프를 끝낸다")
    void apply_noToolCalls_returnsFalse() {
        assertFalse(limiter.apply(withoutToolCalls()));
    }

    @Test
    @DisplayName("도구 호출은 상한까지 허용하고 넘으면 중단한다")
    void apply_overLimit_returnsFalse() {
        for (int round = 1; round <= ToolCallRoundLimiter.MAX_ROUNDS; round++) {
            assertTrue(limiter.apply(withToolCalls()), "상한 이내 라운드 " + round);
        }

        assertFalse(limiter.apply(withToolCalls()));
    }

    @Test
    @DisplayName("루프가 끝나면 카운터가 지워져 다음 질문은 다시 상한만큼 허용된다")
    void apply_counterResetsAfterLoopEnds() {
        for (int round = 1; round <= ToolCallRoundLimiter.MAX_ROUNDS; round++) {
            limiter.apply(withToolCalls());
        }
        assertFalse(limiter.apply(withToolCalls())); // 상한 초과로 중단(카운터 정리)

        assertTrue(limiter.apply(withToolCalls())); // 새 루프 — 다시 1라운드부터

        limiter.apply(withoutToolCalls()); // 정상 종료도 카운터를 지운다
        assertTrue(limiter.apply(withToolCalls()));
    }
}
