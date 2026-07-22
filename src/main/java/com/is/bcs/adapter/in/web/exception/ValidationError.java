package com.is.bcs.adapter.in.web.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 검증 실패 응답 errors[] 배열의 한 항목.
 * 클래스레벨 제약 위반은 특정 필드가 없어 field가 null이 되므로 직렬화에서 키를 생략한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationError(String field, String message) {
}
