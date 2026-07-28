package com.is.bcs.application.port.in.member;

import com.is.bcs.domain.member.District;
import com.is.bcs.domain.member.Position;
import com.is.bcs.domain.member.Team;

public interface CompleteMemberProfileUseCase {

    void complete(Long memberId, Command command);

    record Command(
            String name,
            String phone,
            String email,
            District district,
            Team team,
            Position position
    ) {
    }
}