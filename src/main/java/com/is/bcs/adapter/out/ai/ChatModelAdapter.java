package com.is.bcs.adapter.out.ai;

import com.is.bcs.application.port.out.chat.ChatModelPort;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/** ChatClient로 모델을 호출하는 출력 어댑터 — 도구 호출 루프는 ChatClient가 수행한다. */
@Component
@RequiredArgsConstructor
public class ChatModelAdapter implements ChatModelPort {

    private final ChatClient chatClient;

    @Override
    public String answer(String question) {
        String content = chatClient.prompt()
                .user(question)
                .call()
                .content();
        return content == null ? "" : content;
    }
}
