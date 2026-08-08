package com.is.bcs.application.port.in.chat;

public interface ClearChatHistoryUseCase {

    /** 그 계정의 대화를 비운다. 계정이 없으면 지울 것도 없다. */
    void clear(Long memberId);
}
