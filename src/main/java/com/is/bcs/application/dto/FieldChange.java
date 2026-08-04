package com.is.bcs.application.dto;

/** 갱신되는 항목 하나 — 무엇이 어떤 값에서 어떤 값으로 바뀌는지. */
public record FieldChange(String field, String before, String after) {
}
