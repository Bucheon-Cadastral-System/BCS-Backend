package com.is.bcs.application.port.out.chat;

/** 질문을 모델에 보내 답변 텍스트를 받는 출력 포트. */
public interface ChatModelPort {

    String answer(String question);
}
