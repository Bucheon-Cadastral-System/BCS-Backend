package com.is.bcs.adapter.in.web.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorDetailResolverTest {

    private ErrorDetailResolver resolver;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        resolver = new ErrorDetailResolver(messageSource);
    }

    @Test
    @DisplayName("번들에 키가 있는 status는 해당 문구를 쓴다")
    void detailFor_bundledStatus_usesBundleMessage() {
        assertEquals("요청한 리소스를 찾을 수 없습니다", resolver.detailFor(HttpStatus.NOT_FOUND));
        assertEquals("인증이 필요합니다", resolver.detailFor(HttpStatus.UNAUTHORIZED));
        assertEquals("권한이 없습니다", resolver.detailFor(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("번들에 키가 없는 4xx는 상태군 공통 문구로 폴백한다")
    void detailFor_unbundled4xx_fallsBack() {
        assertEquals("요청을 처리할 수 없습니다", resolver.detailFor(HttpStatus.PAYMENT_REQUIRED));
    }

    @Test
    @DisplayName("번들에 키가 없는 5xx는 상태군 공통 문구로 폴백한다")
    void detailFor_unbundled5xx_fallsBack() {
        assertEquals("서버 내부 오류가 발생했습니다", resolver.detailFor(HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
