package com.is.bcs.adapter.in.web.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

/**
 * 상태 기반 표준 detail 문구의 단일 지점 — 세 직렬화 길목(advice fallback · /error 정규화 ·
 * 보안 필터단 401/403 핸들러)이 공유한다. messages.properties의 error.{status} 키에서 찾고,
 * 없으면 상태군 공통 문구로 폴백한다.
 * (도메인 예외·검증 detail은 개별 내용이 곧 정보라 여기서 균일화하지 않는다)
 */
@Component
@RequiredArgsConstructor
public class ErrorDetailResolver {

    private final MessageSource messageSource;

    public String detailFor(HttpStatusCode status) {
        String fallback = status.is5xxServerError() ? "서버 내부 오류가 발생했습니다" : "요청을 처리할 수 없습니다";
        return messageSource.getMessage("error." + status.value(), null, fallback, LocaleContextHolder.getLocale());
    }
}
