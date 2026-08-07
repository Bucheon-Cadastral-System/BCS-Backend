package com.is.bcs.application.port.in.chat;

import com.is.bcs.domain.chat.ChatMessage;

import java.util.List;

public interface GetChatHistoryUseCase {

    /** 그 계정의 대화를 오래된 것부터 돌려준다. 계정이 없으면 빈 목록이다. */
    List<ChatMessage> getHistory(Long memberId);
}
