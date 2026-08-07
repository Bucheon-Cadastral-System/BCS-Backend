package com.is.bcs.domain.chat;

import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * 대화 한 줄 — 계정 하나가 이어 가는 대화에 속한다.
 * 남기고 나면 고치지 않는 이력이라 전 필드가 불변이고, 새 대화는 그 계정의 줄을 지우는 것으로 표현한다.
 * 계정이 없는 호출(개발용 개방 구간)은 귀속시킬 자리가 없으므로 저장하지 않는다.
 */
@Getter
public class ChatMessage {

    private final Long id;
    private final Long memberId;
    private final ChatRole role;
    private final String text;
    private final OffsetDateTime createdAt; // 저장 전에는 null — 영속 계층이 채운다

    private ChatMessage(Long id, Long memberId, ChatRole role, String text, OffsetDateTime createdAt) {
        this.id = id;
        this.memberId = Objects.requireNonNull(memberId, "회원 ID는 필수입니다.");
        this.role = Objects.requireNonNull(role, "발화 주체는 필수입니다.");
        this.text = Objects.requireNonNull(text, "본문은 필수입니다.");
        this.createdAt = createdAt;
    }

    public static ChatMessage of(Long memberId, ChatRole role, String text) {
        return new ChatMessage(null, memberId, role, text, null);
    }

    public static ChatMessage restore(Long id, Long memberId, ChatRole role, String text, OffsetDateTime createdAt) {
        return new ChatMessage(id, memberId, role, text, createdAt);
    }
}
