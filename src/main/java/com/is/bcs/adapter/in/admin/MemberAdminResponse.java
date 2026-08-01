package com.is.bcs.adapter.in.admin;

import com.is.bcs.application.port.in.admin.GetMemberAdminUseCase;
import com.is.bcs.domain.member.District;
import com.is.bcs.domain.member.MemberRole;
import com.is.bcs.domain.member.MemberStatus;
import com.is.bcs.domain.member.Position;
import com.is.bcs.domain.member.Team;

public record MemberAdminResponse(
        Long id,
        String name,
        String email,
        District district,
        Team team,
        Position position,
        MemberStatus memberStatus,
        MemberRole memberRole
) {

    public static MemberAdminResponse from(GetMemberAdminUseCase.Result result) {
        return new MemberAdminResponse(
                result.id(),
                result.name(),
                result.email(),
                result.district(),
                result.team(),
                result.position(),
                result.memberStatus(),
                result.memberRole()
        );
    }
}