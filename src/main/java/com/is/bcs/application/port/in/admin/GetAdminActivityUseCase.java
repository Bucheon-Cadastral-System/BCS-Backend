package com.is.bcs.application.port.in.admin;

import com.is.bcs.domain.admin.AdminActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;

public interface GetAdminActivityUseCase {

    Page<Result> getActivities(Pageable pageable, Command command);

    record Command(
            AdminActivityType activityType
    ) {
    }

    record Result(
            Long id,
            Long actorAdminId,
            Long targetMemberId,
            AdminActivityType activityType,
            String message,
            OffsetDateTime createdAt
    ) {
    }
}