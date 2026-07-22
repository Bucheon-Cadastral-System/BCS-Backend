package com.is.bcs.adapter.in.web.common;

import java.util.List;

/** 페이지네이션 없는 전체 목록 응답 — {content: [...]} 형태. */
public record ContentResponse<T>(List<T> content) {
}
