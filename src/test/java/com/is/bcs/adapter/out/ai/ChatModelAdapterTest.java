package com.is.bcs.adapter.out.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatModelAdapterTest {

    private final FakeModel model = new FakeModel();
    private final ChatModelAdapter adapter = new ChatModelAdapter(ChatClient.builder(model).build());

    @Test
    @DisplayName("모델 답변 본문을 그대로 돌려준다")
    void answer_returnsContent() {
        model.text = "부천시 지적기준점은 2,146점입니다.";

        assertEquals("부천시 지적기준점은 2,146점입니다.", adapter.answer("기준점 몇 개야?"));
    }

    @Test
    @DisplayName("본문 없이 끝난 응답(라운드 상한 중단 등)은 안내 문구로 대체한다")
    void answer_blankContent_returnsFallbackGuidance() {
        model.text = "";

        String answer = adapter.answer("전부 보여줘");

        assertTrue(answer.contains("범위를 좁혀"));
    }

    /** 모델 페이크 — 준비된 본문을 돌려준다. */
    private static class FakeModel implements ChatModel {

        String text = "";

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
        }
    }
}
