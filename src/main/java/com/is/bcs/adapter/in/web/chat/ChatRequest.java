package com.is.bcs.adapter.in.web.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "질문은 필수입니다.") String message
) {
}
