package com.is.bcs.application.port.out.chat;

import com.is.bcs.domain.chat.ChatMessage;

import java.util.List;

/** 대화 줄을 저장하는 출력 포트 — 질문과 답변이 한 쌍으로 들어온다. */
public interface SaveChatMessagePort {

    void saveAll(List<ChatMessage> messages);
}
