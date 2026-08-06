package com.is.bcs.application.port.out.admin;

import com.is.bcs.domain.admin.AdminActivityType;

public interface RecordAdminActivityPort {

    void record(
            Long actorAdminId,
            Long targetMemberId,
            AdminActivityType activityType,
            String message,
            String actorName,
            String targetName
    );
}