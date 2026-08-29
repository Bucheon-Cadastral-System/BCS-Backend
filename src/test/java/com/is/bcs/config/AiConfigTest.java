package com.is.bcs.config;

import com.is.bcs.adapter.in.ai.ControlPointChatTools;
import com.is.bcs.adapter.in.ai.SurveyChatTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ChatClient 구성 검증 — 모델 페이크로 시스템 프롬프트·도구 등록이 프롬프트에 실리는지 본다. */
class AiConfigTest {

    private final FakeModel model = new FakeModel();
    private final ChatClient chatClient = new AiConfig().chatClient(
            ChatClient.builder(model), new ControlPointChatTools(null), new SurveyChatTools(null, null, null));

    @Test
    @DisplayName("호출 프롬프트에 시스템 프롬프트와 사용자 질문이 실린다")
    void prompt_carriesSystemAndUserMessages() {
        String answer = chatClient.prompt().user("기준점 몇 개야?").call().content();

        assertEquals("답변", answer);
        List<Message> messages = model.lastPrompt.getInstructions();
        Message system = messages.stream().filter(m -> m.getMessageType() == MessageType.SYSTEM)
                .findFirst().orElseThrow();
        assertTrue(system.getText().contains("부천시 지적기준점"));
        assertTrue(system.getText().contains("도구"));
        Message user = messages.stream().filter(m -> m.getMessageType() == MessageType.USER)
                .findFirst().orElseThrow();
        assertEquals("기준점 몇 개야?", user.getText());
    }

    @Test
    @DisplayName("조회 도구 7개가 도구 콜백으로 등록된다 — 쓰기·삭제 유스케이스는 올리지 않는다")
    void prompt_carriesToolCallbacks() {
        chatClient.prompt().user("질문").call().content();

        ToolCallingChatOptions options =
                assertInstanceOf(ToolCallingChatOptions.class, model.lastPrompt.getOptions());
        List<String> toolNames = options.getToolCallbacks().stream()
                .map(callback -> callback.getToolDefinition().name())
                .toList();
        assertEquals(7, toolNames.size());
        assertTrue(toolNames.containsAll(List.of(
                "countControlPoints", "getControlPointByNo", "findControlPoints", "getLastSurveyByPointNo",
                "getSurveyProjects", "getSurveyProgress", "getSurveyRecords")));
    }

    /** 모델 페이크 — 받은 프롬프트를 기록하고 고정 답변을 돌려준다(도구 호출은 하지 않는다). */
    private static class FakeModel implements ChatModel {

        Prompt lastPrompt;

        @Override
        public ChatResponse call(Prompt prompt) {
            this.lastPrompt = prompt;
            return new ChatResponse(List.of(new Generation(new AssistantMessage("답변"))));
        }

        /** 실제 모델(Ollama)처럼 도구 인지 옵션을 기본값으로 줘야 도구 콜백이 프롬프트에 실린다. */
        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }
    }
}
