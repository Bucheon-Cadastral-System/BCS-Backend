package com.is.bcs.application.port.out.member;

import com.is.bcs.domain.member.Member;

import java.time.OffsetDateTime;
import java.util.List;

public interface CleanupExpiredIncompleteMemberPort {

    List<Member> findPendingMembersRequestedBefore(OffsetDateTime cutoff);

    int deleteIfStillExpiredAndIncomplete(Long memberId, OffsetDateTime cutoff);

}