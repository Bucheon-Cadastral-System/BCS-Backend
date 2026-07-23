package com.is.bcs.application.service;

import com.is.bcs.application.port.in.chat.AskChatBotUseCase;
import com.is.bcs.application.port.out.chat.ChatModelPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 챗봇 질문 처리. 트랜잭션을 열지 않는다 — 모델 호출은 수 초 이상 걸릴 수 있어
 * 커넥션을 잡아두면 안 되고, 데이터 접근은 도구가 위임하는 조회 서비스가 각자 연다.
 */
@Service
@RequiredArgsConstructor
public class ChatBotService implements AskChatBotUseCase {

    private final ChatModelPort chatModelPort;

    @Override
    public String ask(String question) {
        return chatModelPort.answer(question);
    }
}
