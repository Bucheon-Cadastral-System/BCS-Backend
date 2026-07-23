package com.is.bcs.adapter.out.ai;

import com.is.bcs.application.port.out.chat.ChatModelPort;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/** ChatClient로 모델을 호출하는 출력 어댑터 — 도구 호출 루프는 ChatClient가 수행한다. */
@Component
@RequiredArgsConstructor
public class ChatModelAdapter implements ChatModelPort {

    /** 라운드 상한 중단 등으로 본문 없이 끝난 응답의 대체 안내. */
    private static final String EMPTY_ANSWER_FALLBACK =
            "질문 범위가 넓어 조회를 끝내지 못했습니다. 범위를 좁혀 다시 질문해 주세요.";

    private final ChatClient chatClient;

    @Override
    public String answer(String question) {
        String content = chatClient.prompt()
                .user(question)
                .call()
                .content();
        return content == null || content.isBlank() ? EMPTY_ANSWER_FALLBACK : content;
    }
}
