package com.is.bcs.application.service;

import com.is.bcs.application.port.in.chat.AskChatBotUseCase;
import com.is.bcs.application.port.in.chat.ClearChatHistoryUseCase;
import com.is.bcs.application.port.in.chat.GetChatHistoryUseCase;
import com.is.bcs.application.port.out.chat.DeleteChatMessagePort;
import com.is.bcs.application.port.out.chat.LoadChatMessagePort;
import com.is.bcs.application.port.out.chat.SaveChatMessagePort;
import com.is.bcs.application.port.out.chat.ChatModelPort;
import com.is.bcs.domain.chat.ChatMessage;
import com.is.bcs.domain.chat.ChatRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 챗봇 질문 처리와 대화 이력.
 *
 * <p>질문 처리에 트랜잭션을 열지 않는다 — 모델 호출은 수 초 이상 걸릴 수 있어 커넥션을 잡아두면 안 되고,
 * 데이터 접근은 도구가 위임하는 조회 서비스와 이력 어댑터가 각자 연다.
 *
 * <p>대화는 계정에 귀속된다. 계정을 확인할 수 없는 호출은 남길 자리가 없어 저장하지 않으며,
 * 그 화면의 대화는 새로고침과 함께 사라진다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBotService implements AskChatBotUseCase, GetChatHistoryUseCase, ClearChatHistoryUseCase {

    /** 화면이 복원하는 대화 길이. */
    private static final int HISTORY_LIMIT = 50;

    /** 계정당 보관 상한 — 복원 길이보다 넉넉히 두되 무한히 쌓이지는 않게 한다. */
    private static final int RETAINED = 100;

    private final ChatModelPort chatModelPort;
    private final SaveChatMessagePort saveChatMessagePort;
    private final LoadChatMessagePort loadChatMessagePort;
    private final DeleteChatMessagePort deleteChatMessagePort;

    @Override
    public String ask(String question, Long memberId) {
        String answer = chatModelPort.answer(question);
        if (memberId != null) {
            record(memberId, question, answer);
        }
        return answer;
    }

    @Override
    public List<ChatMessage> getHistory(Long memberId) {
        if (memberId == null) {
            return List.of();
        }
        return loadChatMessagePort.findRecentByMemberId(memberId, HISTORY_LIMIT);
    }

    @Override
    public void clear(Long memberId) {
        if (memberId == null) {
            return;
        }
        deleteChatMessagePort.deleteByMemberId(memberId);
    }

    /** 답변은 이미 만들어졌으므로 이력 저장이 실패해도 화면에는 답변을 내보낸다. */
    private void record(Long memberId, String question, String answer) {
        try {
            saveChatMessagePort.saveAll(List.of(
                    ChatMessage.of(memberId, ChatRole.USER, question),
                    ChatMessage.of(memberId, ChatRole.ASSISTANT, answer)));
            deleteChatMessagePort.deleteOlderThanRecent(memberId, RETAINED);
        } catch (RuntimeException e) {
            log.warn("대화 이력을 저장하지 못했습니다. memberId={}", memberId, e);
        }
    }
}
