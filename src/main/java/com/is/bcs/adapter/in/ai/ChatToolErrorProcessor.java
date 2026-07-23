package com.is.bcs.adapter.in.ai;

import com.is.bcs.domain.controlpoint.exception.ControlPointNotFoundException;
import com.is.bcs.domain.survey.exception.SurveyProjectNotFoundException;
import com.is.bcs.domain.survey.exception.SurveyRecordNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Set;

/**
 * 도구 실행 예외를 모델에 보낼 표현으로 바꾸는 단일 지점 — 웹의 GlobalExceptionHandler와 같은 역할.
 * 예외를 다시 던지지 않고 항상 {"error": 사유} JSON으로 돌려준다 — 도구 하나의 실패로 채팅을 끊는 대신
 * 모델이 실패 사유를 읽고 자연어로 안내하게 하고, 평문 대신 JSON이라 도구 응답 파싱도 깨지지 않는다.
 * 도메인 조회 실패만 사유를 그대로 싣고, 그 외 예외는 도구 결함이므로 원문을 모델에 중계하지 않는다(로그로만).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatToolErrorProcessor implements ToolExecutionExceptionProcessor {

    private static final Set<Class<? extends RuntimeException>> RELAYED_TO_MODEL = Set.of(
            ControlPointNotFoundException.class,
            SurveyProjectNotFoundException.class,
            SurveyRecordNotFoundException.class);

    private static final String GENERIC_MESSAGE = "도구 호출에 실패했습니다. 잠시 후 다시 시도해 주세요.";

    private final JsonMapper jsonMapper;

    @Override
    public String process(ToolExecutionException exception) {
        Throwable cause = exception.getCause();
        if (cause != null && RELAYED_TO_MODEL.contains(cause.getClass())) {
            return errorJson(cause.getMessage());
        }
        log.error("도구 실행 실패 — 원문은 모델에 중계하지 않는다", exception);
        return errorJson(GENERIC_MESSAGE);
    }

    private String errorJson(String message) {
        try {
            return jsonMapper.writeValueAsString(Map.of("error", message));
        } catch (RuntimeException e) {
            // 직렬화까지 실패해도 여기서 던지면 채팅이 끊긴다 — 최소 JSON을 손으로 보장
            return "{\"error\":\"" + GENERIC_MESSAGE + "\"}";
        }
    }
}
