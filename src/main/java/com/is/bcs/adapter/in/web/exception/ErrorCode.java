package com.is.bcs.adapter.in.web.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 계약 — 응답에 실리는 code와 HTTP status를 한 상수가 함께 보유해 둘의 정합을 보장한다.
 *
 * HttpStatus(web 의존)를 보유하므로 웹 어댑터 층에 둔다. 도메인·애플리케이션 계층은 도메인 예외만 던지고,
 * HTTP 매핑은 GlobalExceptionHandler가 담당한다.
 * 배포된 code 값은 변경하지 않는다(클라이언트가 분기 기준으로 사용) — 새 구분이 필요하면 상수를 추가한다.
 */
public interface ErrorCode {

    /** 응답에 실리는 code (상수명과 동일) */
    String code();

    HttpStatus status();
}
