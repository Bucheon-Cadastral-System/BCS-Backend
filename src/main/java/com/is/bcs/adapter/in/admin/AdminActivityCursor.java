package com.is.bcs.adapter.in.admin;

import java.time.OffsetDateTime;

public record AdminActivityCursor(
        OffsetDateTime createdAt,
        Long id
) {
}