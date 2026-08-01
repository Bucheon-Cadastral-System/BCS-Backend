package com.is.bcs.adapter.in.admin;

import com.is.bcs.application.port.in.admin.GetAdminActivityUseCase;
import com.is.bcs.domain.admin.AdminActivityType;

import java.time.OffsetDateTime;

public record AdminActivityResponse(
        Long id,
        Long actorAdminId,
        Long targetMemberId,
        AdminActivityType activityType,
        String message,
        OffsetDateTime createdAt
) {

    public static AdminActivityResponse from(
            GetAdminActivityUseCase.Result result
    ) {
        return new AdminActivityResponse(
                result.id(),
                result.actorAdminId(),
                result.targetMemberId(),
                result.activityType(),
                result.message(),
                result.createdAt()
        );
    }
}