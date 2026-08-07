package com.is.bcs.application.port.out.chat;

/** 대화 줄을 지우는 출력 포트. */
public interface DeleteChatMessagePort {

    /** 그 계정의 대화를 전부 지운다 — 새 대화 시작. */
    void deleteByMemberId(Long memberId);

    /** 그 계정의 최근 keep 줄만 남기고 지운다 — 보관량이 무한히 늘지 않게 한다. */
    void deleteOlderThanRecent(Long memberId, int keep);
}
