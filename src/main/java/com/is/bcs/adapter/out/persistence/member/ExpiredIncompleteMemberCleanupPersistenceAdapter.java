package com.is.bcs.adapter.out.persistence.member;

import com.is.bcs.application.port.out.member.CleanupExpiredIncompleteMemberPort;
import com.is.bcs.domain.member.Member;
import com.is.bcs.domain.member.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ExpiredIncompleteMemberCleanupPersistenceAdapter
        implements CleanupExpiredIncompleteMemberPort {

    private final MemberJpaRepository memberJpaRepository;

    @Override
    public List<Member> findPendingMembersRequestedBefore(OffsetDateTime cutoff) {
        return memberJpaRepository
                .findAllByStatusAndRequestedAtLessThanEqual(MemberStatus.PENDING, cutoff)
                .stream()
                .map(MemberJpaEntity::toDomain)
                .toList();
    }

    @Override
    public int deleteIfStillExpiredAndIncomplete(Long memberId, OffsetDateTime cutoff) {
        return memberJpaRepository.deleteIfExpiredAndIncomplete(memberId, MemberStatus.PENDING, cutoff);
    }
}