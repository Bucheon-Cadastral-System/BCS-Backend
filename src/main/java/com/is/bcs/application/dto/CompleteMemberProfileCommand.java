package com.is.bcs.application.dto;

import com.is.bcs.domain.member.District;
import com.is.bcs.domain.member.Position;
import com.is.bcs.domain.member.Team;

public record CompleteMemberProfileCommand(
        Long memberId,
        String name,
        String phone,
        String email,
        District district,
        String department,
        Team team,
        Position position
) {
}