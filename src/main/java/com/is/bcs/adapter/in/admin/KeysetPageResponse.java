package com.is.bcs.adapter.in.admin;

import java.util.List;

public record KeysetPageResponse<T>(
        List<T> content,
        String nextCursor,
        boolean hasNext,
        int size
) {
}