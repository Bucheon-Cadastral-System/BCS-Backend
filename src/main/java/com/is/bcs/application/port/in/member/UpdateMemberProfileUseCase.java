package com.is.bcs.application.port.in.member;

import com.is.bcs.domain.member.District;
import com.is.bcs.domain.member.Position;
import com.is.bcs.domain.member.Team;

public interface UpdateMemberProfileUseCase {

    void update(Long memberId, Command command);

    record Command(
            String phone,
            District district,
            Team team,
            Position position
    ) {
    }

}
