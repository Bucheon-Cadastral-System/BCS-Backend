package com.is.bcs.adapter.in.member;

import com.is.bcs.application.port.in.member.GetMyProfileUseCase;
import com.is.bcs.domain.member.District;
import com.is.bcs.domain.member.MemberRole;
import com.is.bcs.domain.member.Position;
import com.is.bcs.domain.member.Team;

/**
 * 다른 회원의 신원 — 화면이 이름 옆에서 그 사람이 누구인지 보일 때 쓴다.
 *
 * <p>가입 상태는 싣지 않는다. 승인·비활성은 관리자 화면이 다루는 값이고, 이름을 눌러 본 사람이 알 값이 아니다.
 */
public record MemberSummaryResponse(
        Long id,
        String name,
        String phone,
        String email,
        District district,
        String department,
        Team team,
        Position position,
        MemberRole role
) {

    public static MemberSummaryResponse from(GetMyProfileUseCase.Result result) {
        return new MemberSummaryResponse(
                result.id(),
                result.name(),
                result.phone(),
                result.email(),
                result.district(),
                result.department(),
                result.team(),
                result.position(),
                result.role()
        );
    }
}
