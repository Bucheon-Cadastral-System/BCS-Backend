package com.is.bcs.application.port.in.chat;

public interface AskChatBotUseCase {

    /** memberId 가 있으면 질문과 답변을 그 계정의 대화로 남긴다. 없으면 답변만 돌려준다. */
    String ask(String question, Long memberId);
}
