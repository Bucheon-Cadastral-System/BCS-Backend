package com.is.bcs.adapter.in.web.chat;

/**
 * 챗봇 답변.
 *
 * @param answer 모델이 만든 답
 * @param elapsedMs 질문을 받아 답을 낼 때까지 걸린 시간 — 화면이 답변 아래에 적는다
 */
public record ChatResponse(String answer, long elapsedMs) {
}
