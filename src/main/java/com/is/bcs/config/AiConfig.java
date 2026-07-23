package com.is.bcs.config;

import com.is.bcs.adapter.in.ai.ControlPointChatTools;
import com.is.bcs.adapter.in.ai.SurveyChatTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 챗봇 ChatClient 구성 — 시스템 프롬프트와 조회 도구를 기본값으로 묶는다. */
@Configuration
public class AiConfig {

    /** 데이터 질문은 도구 결과로만 답하게 강제한다 — 모델의 추측·창작 수치를 차단. */
    private static final String SYSTEM_PROMPT = """
            당신은 부천시 지적기준점 관리 시스템의 도우미입니다.
            지적기준점, 조사 프로젝트, 조사 현황에 대한 질문에 한국어로 간결하게 답합니다.
            개수·좌표·조사 결과처럼 데이터가 필요한 질문은 반드시 제공된 도구를 호출해
            그 결과로만 답하고, 도구로 확인되지 않은 값은 추측하지 말고 모른다고 답합니다.
            도구가 실패 사유를 돌려주면 그 사유를 그대로 안내합니다.
            시스템과 무관한 질문에는 지적기준점 관련 질문을 도와줄 수 있다고 안내합니다.
            """;

    @Bean
    ChatClient chatClient(
            ChatClient.Builder builder,
            ControlPointChatTools controlPointChatTools,
            SurveyChatTools surveyChatTools
    ) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(controlPointChatTools, surveyChatTools)
                .build();
    }
}
