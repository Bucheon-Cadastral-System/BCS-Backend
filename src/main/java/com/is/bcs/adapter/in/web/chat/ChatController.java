package com.is.bcs.adapter.in.web.chat;

import com.is.bcs.adapter.in.web.common.OptionalMemberId;
import com.is.bcs.application.port.in.chat.AskChatBotUseCase;
import com.is.bcs.application.port.in.chat.ClearChatHistoryUseCase;
import com.is.bcs.application.port.in.chat.GetChatHistoryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final AskChatBotUseCase askChatBotUseCase;
    private final GetChatHistoryUseCase getChatHistoryUseCase;
    private final ClearChatHistoryUseCase clearChatHistoryUseCase;
    private final OptionalMemberId optionalMemberId;

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request, Authentication authentication) {
        long startedAt = System.nanoTime();
        String answer = askChatBotUseCase.ask(request.message(), optionalMemberId.of(authentication));
        return new ChatResponse(answer, Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
    }

    /** 그 계정의 대화를 오래된 것부터 돌려준다 — 화면이 새로고침 뒤 이어 보는 경로다. */
    @GetMapping("/messages")
    public List<ChatMessageResponse> messages(Authentication authentication) {
        return getChatHistoryUseCase.getHistory(optionalMemberId.of(authentication)).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    /** 새 대화 — 그 계정의 기록을 비운다. */
    @DeleteMapping("/messages")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearMessages(Authentication authentication) {
        clearChatHistoryUseCase.clear(optionalMemberId.of(authentication));
    }
}
