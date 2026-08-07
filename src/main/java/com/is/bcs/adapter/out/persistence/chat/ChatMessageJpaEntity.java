package com.is.bcs.adapter.out.persistence.chat;

import com.is.bcs.adapter.out.persistence.common.BaseCreatedTime;
import com.is.bcs.adapter.out.persistence.common.EntityReferences;
import com.is.bcs.adapter.out.persistence.member.MemberJpaEntity;
import com.is.bcs.domain.chat.ChatMessage;
import com.is.bcs.domain.chat.ChatRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Table(
        name = "chat_messages",
        schema = "bcs",
        // 조회는 언제나 한 계정의 최근 줄이라 (회원, id) 순서로 잡는다
        indexes = @Index(name = "idx_chat_messages_member_id", columnList = "member_id, id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageJpaEntity extends BaseCreatedTime {

    @Id
    // 질문과 답변을 한 번에 넣으므로 시퀀스로 id 를 미리 받아 INSERT 를 묶는다
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chat_messages_seq")
    @SequenceGenerator(name = "chat_messages_seq", sequenceName = "chat_messages_seq", schema = "bcs", allocationSize = 50)
    private Long id;

    /** 대화 주인 — 업무 데이터와 달리 그 계정의 것이라 회원이 사라지면 함께 사라진다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_messages_member"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private MemberJpaEntity member;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ChatRole role;

    // 답변에 표·차트 블록이 통째로 들어와 길이 상한을 두지 않는다
    @Column(name = "text", nullable = false, columnDefinition = "text")
    private String text;

    private ChatMessageJpaEntity(MemberJpaEntity member, ChatRole role, String text) {
        this.member = member;
        this.role = role;
        this.text = text;
    }

    static ChatMessageJpaEntity from(ChatMessage message, EntityManager entityManager) {
        return new ChatMessageJpaEntity(
                EntityReferences.of(entityManager, MemberJpaEntity.class, message.getMemberId()),
                message.getRole(), message.getText());
    }

    ChatMessage toDomain() {
        return ChatMessage.restore(id, member.getId(), role, text, getCreatedAt());
    }
}
