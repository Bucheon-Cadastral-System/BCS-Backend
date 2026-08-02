package com.is.bcs.adapter.out.persistence.admin;

import com.is.bcs.application.port.out.admin.RecordAdminActivityPort;
import com.is.bcs.domain.admin.AdminActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
@RequiredArgsConstructor
public class RecordAdminActivityAdapter implements RecordAdminActivityPort {

    private final AdminActivityLogJpaRepository adminActivityLogJpaRepository;

    @Override
    public void record(Long actorAdminId, Long targetMemberId, AdminActivityType activityType, String message) {
        AdminActivityLogJpaEntity log =
                new AdminActivityLogJpaEntity(
                        actorAdminId,
                        targetMemberId,
                        activityType,
                        message,
                        OffsetDateTime.now()
                );

        adminActivityLogJpaRepository.save(log);
    }
}