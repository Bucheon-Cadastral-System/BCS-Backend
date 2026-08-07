package com.is.bcs.application.port.out.chat;

import com.is.bcs.domain.chat.ChatMessage;

import java.util.List;

/** 대화 줄을 읽는 출력 포트. */
public interface LoadChatMessagePort {

    /** 그 계정의 최근 limit 줄을 오래된 것부터 돌려준다 — 화면이 위에서 아래로 그대로 그린다. */
    List<ChatMessage> findRecentByMemberId(Long memberId, int limit);
}
