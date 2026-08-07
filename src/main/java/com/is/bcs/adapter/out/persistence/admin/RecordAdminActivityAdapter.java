package com.is.bcs.adapter.out.persistence.admin;

import com.is.bcs.application.port.out.admin.RecordAdminActivityPort;
import com.is.bcs.domain.admin.AdminActivityType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
@RequiredArgsConstructor
public class RecordAdminActivityAdapter implements RecordAdminActivityPort {

    @PersistenceContext
    private EntityManager entityManager;


    private final AdminActivityLogJpaRepository adminActivityLogJpaRepository;

    // 시각은 TimeConfig 의 Clock 하나에서만 얻는다 — JVM 기본 타임존과 무관하게 KST
    private final Clock clock;

    @Override
    public void record(Long actorAdminId, Long targetMemberId, AdminActivityType activityType, String message
            , String actorName, String targetName) {
        AdminActivityLogJpaEntity log =
                AdminActivityLogJpaEntity.of(
                        actorAdminId,
                        targetMemberId,
                        activityType,
                        message,
                        actorName,
                        targetName,
                        OffsetDateTime.now(clock),
                        entityManager
                );

        adminActivityLogJpaRepository.save(log);
    }
}