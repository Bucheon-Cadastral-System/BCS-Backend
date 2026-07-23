package com.is.bcs.application.service;

import com.is.bcs.application.port.out.chat.ChatModelPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatBotServiceTest {

    private final FakeChatModel model = new FakeChatModel();
    private final ChatBotService service = new ChatBotService(model);

    @Test
    @DisplayName("질문을 그대로 모델 포트에 전달하고 답변을 돌려준다")
    void ask_delegatesToModelPort() {
        model.answer = "부천시 지적기준점은 2,146점입니다.";

        String answer = service.ask("기준점 몇 개야?");

        assertEquals("기준점 몇 개야?", model.askedQuestion);
        assertEquals("부천시 지적기준점은 2,146점입니다.", answer);
    }

    /** 모델 포트 페이크 — 전달된 질문을 기록하고 준비된 답변을 돌려준다. */
    private static class FakeChatModel implements ChatModelPort {

        String askedQuestion;
        String answer;

        @Override
        public String answer(String question) {
            this.askedQuestion = question;
            return answer;
        }
    }
}
