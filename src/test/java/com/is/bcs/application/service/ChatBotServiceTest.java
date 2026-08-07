package com.is.bcs.application.service;

import com.is.bcs.application.port.out.chat.ChatModelPort;
import com.is.bcs.application.port.out.chat.DeleteChatMessagePort;
import com.is.bcs.application.port.out.chat.LoadChatMessagePort;
import com.is.bcs.application.port.out.chat.SaveChatMessagePort;
import com.is.bcs.domain.chat.ChatMessage;
import com.is.bcs.domain.chat.ChatRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 챗봇 서비스 검증 — 답변과 대화 보관이 계정 단위로 갈리는지 본다. */
class ChatBotServiceTest {

    private static final Long MEMBER = 7L;

    private final FakeChatModel model = new FakeChatModel();
    private final FakeChatHistory history = new FakeChatHistory();
    private final ChatBotService service = new ChatBotService(model, history, history, history);

    @Test
    @DisplayName("질문을 그대로 모델 포트에 전달하고 답변을 돌려준다")
    void ask_delegatesToModelPort() {
        String answer = service.ask("기준점 몇 개야?", MEMBER);

        assertEquals("기준점 몇 개야?", model.askedQuestion);
        assertEquals("답변: 기준점 몇 개야?", answer);
    }

    @Test
    @DisplayName("계정이 있으면 질문과 답변을 그 계정의 대화로 남긴다")
    void ask_withMember_recordsBothSides() {
        String answer = service.ask("기준점 몇 개야?", MEMBER);

        assertEquals("답변: 기준점 몇 개야?", answer);
        assertEquals(2, history.saved.size());
        assertEquals(ChatRole.USER, history.saved.getFirst().getRole());
        assertEquals("기준점 몇 개야?", history.saved.getFirst().getText());
        assertEquals(ChatRole.ASSISTANT, history.saved.getLast().getRole());
        assertEquals(answer, history.saved.getLast().getText());
        assertTrue(history.saved.stream().allMatch(message -> MEMBER.equals(message.getMemberId())));
    }

    @Test
    @DisplayName("계정이 없으면 답변만 하고 남기지 않는다 — 귀속시킬 자리가 없다")
    void ask_withoutMember_doesNotRecord() {
        String answer = service.ask("기준점 몇 개야?", null);

        assertEquals("답변: 기준점 몇 개야?", answer);
        assertTrue(history.saved.isEmpty());
        assertEquals(0, history.trimmedTo);
    }

    @Test
    @DisplayName("보관 상한을 넘긴 오래된 줄은 저장 직후 정리한다")
    void ask_trimsBeyondRetention() {
        service.ask("기준점 몇 개야?", MEMBER);

        assertEquals(MEMBER, history.trimmedMemberId);
        assertEquals(100, history.trimmedTo);
    }

    @Test
    @DisplayName("이력 저장이 실패해도 답변은 돌려준다 — 답은 이미 만들어졌다")
    void ask_saveFailure_stillAnswers() {
        history.failOnSave = true;

        assertEquals("답변: 기준점 몇 개야?", service.ask("기준점 몇 개야?", MEMBER));
    }

    @Test
    @DisplayName("이력 조회는 그 계정의 최근 50줄을 요청한다")
    void getHistory_readsRecentOfMember() {
        history.stored.add(ChatMessage.of(MEMBER, ChatRole.USER, "지난 질문"));

        List<ChatMessage> loaded = service.getHistory(MEMBER);

        assertEquals(1, loaded.size());
        assertEquals(MEMBER, history.loadedMemberId);
        assertEquals(50, history.loadedLimit);
    }

    @Test
    @DisplayName("계정이 없으면 이력은 빈 목록이고 저장소를 건드리지 않는다")
    void getHistory_withoutMember_returnsEmpty() {
        history.stored.add(ChatMessage.of(MEMBER, ChatRole.USER, "지난 질문"));

        assertTrue(service.getHistory(null).isEmpty());
        assertNull(history.loadedMemberId);
    }

    @Test
    @DisplayName("새 대화는 그 계정의 기록만 지운다")
    void clear_deletesOnlyThatMember() {
        service.clear(MEMBER);

        assertEquals(MEMBER, history.clearedMemberId);
    }

    @Test
    @DisplayName("계정이 없으면 비울 것도 없다")
    void clear_withoutMember_doesNothing() {
        service.clear(null);

        assertNull(history.clearedMemberId);
    }

    /** 모델 페이크 — 전달된 질문을 기록하고 그 질문을 실은 답변을 돌려준다. */
    private static class FakeChatModel implements ChatModelPort {

        String askedQuestion;

        @Override
        public String answer(String question) {
            this.askedQuestion = question;
            return "답변: " + question;
        }
    }

    /** 대화 보관 페이크 — 저장·조회·삭제 세 포트를 함께 대신한다. */
    private static class FakeChatHistory implements SaveChatMessagePort, LoadChatMessagePort, DeleteChatMessagePort {

        final List<ChatMessage> saved = new ArrayList<>();
        final List<ChatMessage> stored = new ArrayList<>();
        boolean failOnSave;
        Long loadedMemberId;
        int loadedLimit;
        Long clearedMemberId;
        Long trimmedMemberId;
        int trimmedTo;

        @Override
        public void saveAll(List<ChatMessage> messages) {
            if (failOnSave) {
                throw new IllegalStateException("저장 실패");
            }
            saved.addAll(messages);
        }

        @Override
        public List<ChatMessage> findRecentByMemberId(Long memberId, int limit) {
            loadedMemberId = memberId;
            loadedLimit = limit;
            return stored.stream().filter(message -> memberId.equals(message.getMemberId())).toList();
        }

        @Override
        public void deleteByMemberId(Long memberId) {
            clearedMemberId = memberId;
            stored.removeIf(message -> memberId.equals(message.getMemberId()));
        }

        @Override
        public void deleteOlderThanRecent(Long memberId, int keep) {
            trimmedMemberId = memberId;
            trimmedTo = keep;
        }
    }
}
