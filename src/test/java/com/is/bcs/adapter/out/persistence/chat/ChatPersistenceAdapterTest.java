package com.is.bcs.adapter.out.persistence.chat;

import com.is.bcs.application.port.out.chat.DeleteChatMessagePort;
import com.is.bcs.application.port.out.chat.LoadChatMessagePort;
import com.is.bcs.application.port.out.chat.SaveChatMessagePort;
import com.is.bcs.application.port.out.member.SaveMemberPort;
import com.is.bcs.domain.chat.ChatMessage;
import com.is.bcs.domain.chat.ChatRole;
import com.is.bcs.domain.member.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 대화 이력 영속 왕복 검증 — DB 필요(bcs/docker-compose). */
@SpringBootTest
@Transactional
class ChatPersistenceAdapterTest {

    private static final OffsetDateTime JOINED_AT = OffsetDateTime.parse("2026-08-01T09:00:00+09:00");

    @Autowired
    private SaveChatMessagePort saveChatMessagePort;

    @Autowired
    private LoadChatMessagePort loadChatMessagePort;

    @Autowired
    private DeleteChatMessagePort deleteChatMessagePort;

    @Autowired
    private SaveMemberPort memberPort;

    private int memberSeq;

    private Long member() {
        memberSeq++;
        return memberPort.save(Member.registerWithKakao("kakao-chat-" + memberSeq, JOINED_AT)).getId();
    }

    private void say(Long memberId, String... texts) {
        for (String text : texts) {
            saveChatMessagePort.saveAll(List.of(ChatMessage.of(memberId, ChatRole.USER, text)));
        }
    }

    @Test
    @DisplayName("대화는 오래된 것부터 돌아온다 — 화면이 위에서 아래로 그대로 그린다")
    void findRecent_returnsOldestFirst() {
        Long memberId = member();
        say(memberId, "첫 질문", "둘째 질문", "셋째 질문");

        List<ChatMessage> loaded = loadChatMessagePort.findRecentByMemberId(memberId, 50);

        assertEquals(List.of("첫 질문", "둘째 질문", "셋째 질문"), loaded.stream().map(ChatMessage::getText).toList());
    }

    @Test
    @DisplayName("limit 은 최근 줄을 남긴다 — 잘려 나가는 것은 앞선 대화다")
    void findRecent_keepsLatestWithinLimit() {
        Long memberId = member();
        say(memberId, "첫 질문", "둘째 질문", "셋째 질문");

        List<ChatMessage> loaded = loadChatMessagePort.findRecentByMemberId(memberId, 2);

        assertEquals(List.of("둘째 질문", "셋째 질문"), loaded.stream().map(ChatMessage::getText).toList());
    }

    @Test
    @DisplayName("다른 계정의 대화는 섞이지 않는다")
    void findRecent_isolatesMembers() {
        Long mine = member();
        Long other = member();
        say(mine, "내 질문");
        say(other, "남의 질문");

        assertEquals(List.of("내 질문"), loadChatMessagePort.findRecentByMemberId(mine, 50).stream()
                .map(ChatMessage::getText).toList());
        assertEquals(List.of("남의 질문"), loadChatMessagePort.findRecentByMemberId(other, 50).stream()
                .map(ChatMessage::getText).toList());
    }

    @Test
    @DisplayName("보관 상한을 넘기면 오래된 줄부터 지워지고 남의 대화는 건드리지 않는다")
    void deleteOlderThanRecent_trimsOwnOnly() {
        Long mine = member();
        Long other = member();
        say(mine, "하나", "둘", "셋", "넷");
        say(other, "남의 하나", "남의 둘");

        deleteChatMessagePort.deleteOlderThanRecent(mine, 2);

        assertEquals(List.of("셋", "넷"), loadChatMessagePort.findRecentByMemberId(mine, 50).stream()
                .map(ChatMessage::getText).toList());
        assertEquals(2, loadChatMessagePort.findRecentByMemberId(other, 50).size());
    }

    @Test
    @DisplayName("새 대화는 그 계정의 기록만 비운다")
    void deleteByMemberId_clearsOwnOnly() {
        Long mine = member();
        Long other = member();
        say(mine, "내 질문");
        say(other, "남의 질문");

        deleteChatMessagePort.deleteByMemberId(mine);

        assertTrue(loadChatMessagePort.findRecentByMemberId(mine, 50).isEmpty());
        assertEquals(1, loadChatMessagePort.findRecentByMemberId(other, 50).size());
    }
}
