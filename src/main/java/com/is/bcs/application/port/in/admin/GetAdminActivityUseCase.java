package com.is.bcs.application.port.in.admin;

import com.is.bcs.domain.admin.AdminActivityType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.OffsetDateTime;

public interface GetAdminActivityUseCase {

    Slice<Result> getActivities(Pageable pageable, Command command);

    record Command(
            AdminActivityType activityType,
            OffsetDateTime cursorCreatedAt,
            Long cursorId
    ) {
    }

    record Result(
            Long id,
            Long actorAdminId,
            Long targetMemberId,
            AdminActivityType activityType,
            String message,
            String actorName,
            String targetName,
            OffsetDateTime createdAt
    ) {
    }
}