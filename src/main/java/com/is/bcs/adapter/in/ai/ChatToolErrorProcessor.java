package com.is.bcs.adapter.in.ai;

import com.is.bcs.domain.controlpoint.exception.ControlPointNotFoundException;
import com.is.bcs.domain.survey.exception.SurveyProjectNotFoundException;
import com.is.bcs.domain.survey.exception.SurveyRecordNotFoundException;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 도구 실행 예외를 모델에 보낼 표현으로 바꾸는 단일 지점 — 웹의 GlobalExceptionHandler와 같은 역할.
 * 도메인 조회 실패만 사유 문자열로 모델에 전달해 챗봇이 안내하게 하고,
 * 그 외 예외는 도구 결함이므로 다시 던져 표준 오류 응답 경로로 보낸다(프레임워크 원문을 모델에 중계하지 않는다).
 */
@Component
public class ChatToolErrorProcessor implements ToolExecutionExceptionProcessor {

    private static final Set<Class<? extends RuntimeException>> RELAYED_TO_MODEL = Set.of(
            ControlPointNotFoundException.class,
            SurveyProjectNotFoundException.class,
            SurveyRecordNotFoundException.class);

    @Override
    public String process(ToolExecutionException exception) {
        Throwable cause = exception.getCause();
        if (cause != null && RELAYED_TO_MODEL.contains(cause.getClass())) {
            return cause.getMessage();
        }
        throw exception;
    }
}
