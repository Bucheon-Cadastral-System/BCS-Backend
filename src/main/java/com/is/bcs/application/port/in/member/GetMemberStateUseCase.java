package com.is.bcs.application.port.in.member;

import com.is.bcs.domain.member.MemberStatus;

public interface GetMemberStateUseCase {

    Result getState(Long memberId);

    record Result(
            MemberStatus status,
            boolean profileCompleted
    ) {
    }
}