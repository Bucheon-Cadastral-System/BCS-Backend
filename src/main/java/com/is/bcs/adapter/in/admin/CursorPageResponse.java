package com.is.bcs.adapter.in.admin;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> content,
        String nextCursor,
        boolean hasNext,
        int size
) {
}