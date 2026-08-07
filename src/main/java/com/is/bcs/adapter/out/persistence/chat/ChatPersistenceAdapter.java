package com.is.bcs.adapter.out.persistence.chat;

import com.is.bcs.application.port.out.chat.DeleteChatMessagePort;
import com.is.bcs.application.port.out.chat.LoadChatMessagePort;
import com.is.bcs.application.port.out.chat.SaveChatMessagePort;
import com.is.bcs.domain.chat.ChatMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 대화 이력 영속 어댑터.
 *
 * <p>쓰기 트랜잭션을 서비스가 아니라 여기서 연다 — 질문 처리는 모델 응답을 수 초 기다리므로,
 * 서비스에 트랜잭션을 걸면 그동안 커넥션을 잡고 있게 된다.
 */
@Component
@RequiredArgsConstructor
public class ChatPersistenceAdapter implements SaveChatMessagePort, LoadChatMessagePort, DeleteChatMessagePort {

    // 연관을 껍데기 참조로 만들려면 EntityManager 가 필요하다 — 저장 경로가 상대 행을 읽지 않게 한다
    @PersistenceContext
    private EntityManager entityManager;


    private final ChatMessageJpaRepository chatMessageRepository;

    @Override
    @Transactional
    public void saveAll(List<ChatMessage> messages) {
        chatMessageRepository.saveAll(messages.stream().map(m -> ChatMessageJpaEntity.from(m, entityManager)).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> findRecentByMemberId(Long memberId, int limit) {
        List<ChatMessage> recent = new ArrayList<>(
                chatMessageRepository.findByMemberIdOrderByIdDesc(memberId, Limit.of(limit)).stream()
                        .map(ChatMessageJpaEntity::toDomain)
                        .toList());
        Collections.reverse(recent); // 최근 줄부터 잘라 낸 뒤 화면 순서(오래된 것부터)로 되돌린다
        return recent;
    }

    @Override
    @Transactional
    public void deleteByMemberId(Long memberId) {
        chatMessageRepository.deleteByMemberId(memberId);
    }

    @Override
    @Transactional
    public void deleteOlderThanRecent(Long memberId, int keep) {
        chatMessageRepository.deleteOlderThanRecent(memberId, keep);
    }
}
