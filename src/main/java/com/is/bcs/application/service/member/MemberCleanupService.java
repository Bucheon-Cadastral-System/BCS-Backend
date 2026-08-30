package com.is.bcs.application.service.member;

import com.is.bcs.application.port.in.member.CleanupExpiredIncompleteMembersUseCase;
import com.is.bcs.application.port.out.member.CleanupExpiredIncompleteMemberPort;
import com.is.bcs.domain.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberCleanupService implements CleanupExpiredIncompleteMembersUseCase {

    private static final long RETENTION_DAYS = 7L;

    private final CleanupExpiredIncompleteMemberPort cleanupMemberPort;
    private final Clock clock;

    @Override
    @Transactional
    public int cleanup() {
        OffsetDateTime cutoff = OffsetDateTime.now(clock).minusDays(RETENTION_DAYS); // 현재 시간으로부터 7일전 계산

        // 1. 우선적으로 삭제 대상 조회
        List<Member> candidates = cleanupMemberPort.findPendingMembersRequestedBefore(cutoff);

        int deletedCount = 0;

        // 2. 회원 가입 미작성한 경우 삭제 이터레이터
        for (Member member : candidates) {
            if (member.isProfileCompleted()) {
                continue;
            }
            deletedCount += cleanupMemberPort.deleteIfStillExpiredAndIncomplete(member.getId(), cutoff);
        }

        return deletedCount;
    }
}