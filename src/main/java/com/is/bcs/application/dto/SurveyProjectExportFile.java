package com.is.bcs.application.dto;

/**
 * 내보낸 조사 대상 파일 — 내용과 저장 이름.
 *
 * @param content  파일 내용
 * @param fileName 사용자가 저장할 이름(확장자 포함)
 */
public record SurveyProjectExportFile(byte[] content, String fileName) {
}
