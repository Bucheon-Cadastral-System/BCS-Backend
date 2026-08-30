package com.is.bcs.adapter.in.member;

import com.is.bcs.application.port.in.admin.GetMemberAdminUseCase;
import com.is.bcs.application.port.in.member.GetMyProfileUseCase;
import com.is.bcs.domain.member.District;
import com.is.bcs.domain.member.MemberRole;
import com.is.bcs.domain.member.MemberStatus;
import com.is.bcs.domain.member.Position;
import com.is.bcs.domain.member.Team;

public record MemberProfileResponse(
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
        String profileImageUrl
) {

    public static MemberProfileResponse from(GetMyProfileUseCase.Result result) {
        return new MemberProfileResponse(
                result.id(),
                result.name(),
                result.phone(),
                result.email(),
                result.district(),
                result.department(),
                result.team(),
                result.position(),
                result.role(),
                result.status(),
                toProfileImageUrl(result)
        );
    }

    private static String toProfileImageUrl(GetMyProfileUseCase.Result result) {
        if (!result.profileImageRegistered()) {
            return null;
        }

        return "/api/members/%d/profile-image".formatted(result.id());
    }


}
