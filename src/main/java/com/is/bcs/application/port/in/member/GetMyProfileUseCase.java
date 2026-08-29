package com.is.bcs.application.port.in.member;

import com.is.bcs.domain.member.District;
import com.is.bcs.domain.member.MemberRole;
import com.is.bcs.domain.member.MemberStatus;
import com.is.bcs.domain.member.Position;
import com.is.bcs.domain.member.Team;

public interface GetMyProfileUseCase {

    Result getProfile(Long memberId);

    record Result(
            Long id,
            String name,
            String phone,
            String email,
            District district,
            String department,
            Team team,
            Position position,
            MemberRole role,
            MemberStatus status,
            boolean profileImageRegistered
    ) {
    }
}
