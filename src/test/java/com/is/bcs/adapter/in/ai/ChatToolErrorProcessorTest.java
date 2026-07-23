package com.is.bcs.adapter.in.ai;

import com.is.bcs.domain.controlpoint.exception.ControlPointNotFoundException;
import com.is.bcs.domain.survey.exception.SurveyProjectNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatToolErrorProcessorTest {

    private final ChatToolErrorProcessor processor = new ChatToolErrorProcessor();

    private static ToolExecutionException toolException(Throwable cause) {
        return new ToolExecutionException(
                DefaultToolDefinition.builder().name("tool").description("도구").inputSchema("{}").build(),
                cause);
    }

    @Test
    @DisplayName("도메인 조회 실패는 사유 문자열로 바꿔 모델에 전달한다")
    void process_domainNotFound_returnsMessage() {
        assertEquals("기준점을 찾을 수 없습니다: 41192D999999999",
                processor.process(toolException(
                        new ControlPointNotFoundException("기준점을 찾을 수 없습니다: 41192D999999999"))));
        assertEquals("조사 프로젝트를 찾을 수 없습니다: 99",
                processor.process(toolException(
                        new SurveyProjectNotFoundException("조사 프로젝트를 찾을 수 없습니다: 99"))));
    }

    @Test
    @DisplayName("그 외 예외는 모델에 중계하지 않고 다시 던진다")
    void process_unexpected_rethrows() {
        ToolExecutionException exception = toolException(new IllegalStateException("도구 결함"));

        ToolExecutionException thrown =
                assertThrows(ToolExecutionException.class, () -> processor.process(exception));
        assertSame(exception, thrown);
    }
}
