package com.is.bcs.adapter.in.ai;

import com.is.bcs.domain.controlpoint.exception.ControlPointNotFoundException;
import com.is.bcs.domain.survey.exception.SurveyProjectNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ChatToolErrorProcessorTest {

    private final ChatToolErrorProcessor processor = new ChatToolErrorProcessor(JsonMapper.builder().build());

    private static ToolExecutionException toolException(Throwable cause) {
        return new ToolExecutionException(
                DefaultToolDefinition.builder().name("tool").description("도구").inputSchema("{}").build(),
                cause);
    }

    @Test
    @DisplayName("도메인 조회 실패는 사유를 {\"error\": …} JSON으로 감싸 모델에 전달한다")
    void process_domainNotFound_returnsErrorJson() {
        assertEquals("{\"error\":\"기준점을 찾을 수 없습니다: 41192D999999999\"}",
                processor.process(toolException(
                        new ControlPointNotFoundException("기준점을 찾을 수 없습니다: 41192D999999999"))));
        assertEquals("{\"error\":\"조사 프로젝트를 찾을 수 없습니다: 99\"}",
                processor.process(toolException(
                        new SurveyProjectNotFoundException("조사 프로젝트를 찾을 수 없습니다: 99"))));
    }

    @Test
    @DisplayName("그 외 예외는 원문을 모델에 중계하지 않고 일반 문구 JSON으로 대체한다")
    void process_unexpected_returnsGenericJsonWithoutOriginalMessage() {
        String result = processor.process(toolException(new IllegalStateException("NullPointerException at …")));

        assertEquals("{\"error\":\"도구 호출에 실패했습니다. 잠시 후 다시 시도해 주세요.\"}", result);
        assertFalse(result.contains("NullPointerException"));
    }
}
