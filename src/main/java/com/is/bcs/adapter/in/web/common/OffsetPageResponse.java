package com.is.bcs.adapter.in.web.common;

import org.springframework.data.domain.Page;

import java.util.List;

/** 페이지 응답 봉투 — Spring Data Page 를 화면이 읽는 모양으로 옮긴다. 커서 방식은 KeysetPageResponse 가 따로 있다. */
public record OffsetPageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> OffsetPageResponse<T> from(Page<T> page) {
        return new OffsetPageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}