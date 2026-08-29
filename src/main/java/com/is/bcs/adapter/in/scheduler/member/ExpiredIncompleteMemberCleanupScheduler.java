package com.is.bcs.adapter.in.scheduler.member;

import com.is.bcs.application.port.in.member.CleanupExpiredIncompleteMembersUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredIncompleteMemberCleanupScheduler {

    private final CleanupExpiredIncompleteMembersUseCase cleanupUseCase;

    @Scheduled(
            cron = "${app.member-cleanup.cron:0 0 3 * * *}", // 매일 오전 3시
            zone = "${app.member-cleanup.zone:Asia/Seoul}"
    )
    public void cleanup() {
        int deletedCount = cleanupUseCase.cleanup();
        if(deletedCount > 0) {
            log.info("deleted expired incomplete member cleanup count : {}", deletedCount);
        }
    }
}