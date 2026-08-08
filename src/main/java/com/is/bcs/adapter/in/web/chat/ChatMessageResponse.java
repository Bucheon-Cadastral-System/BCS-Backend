package com.is.bcs.adapter.in.web.chat;

import com.is.bcs.domain.chat.ChatMessage;

import java.util.Locale;

/** 대화 한 줄(화면용) — 화면이 쓰는 소문자 역할 이름으로 내려 준다. */
public record ChatMessageResponse(String role, String text) {

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(message.getRole().name().toLowerCase(Locale.ROOT), message.getText());
    }
}
