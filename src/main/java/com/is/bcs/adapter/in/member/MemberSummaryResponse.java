package com.is.bcs.adapter.in.member;

import com.is.bcs.application.port.in.member.GetMyProfileUseCase;
import com.is.bcs.domain.member.District;
import com.is.bcs.domain.member.Position;
import com.is.bcs.domain.member.Team;

/**
 * 다른 회원의 신원 — 화면이 이름 옆에서 그 사람이 누구인지 보이고, 그 자리에서 연락할 때 쓴다.
 *
 * <p>권한은 싣지 않는다. 이름을 눌러 본 사람이 알 값이 아니고, 관리자 표시는 관리자 화면이 다룬다.
 * 가입 상태도 같은 이유로 싣지 않는다.
 *
 * <p>인증만 하면 누구나 회원 번호를 바꿔 가며 물을 수 있는 경로다. 여기에 칸을 더하면 그 값이 전 회원분
 * 열리는 것과 같으므로, 칸이 늘면 시험이 깨지게 두었다(MemberSummaryResponseTest).
 */
public record MemberSummaryResponse(
        Long id,
        String name,
        String phone,
        String email,
        District district,
        String department,
        Team team,
        Position position
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
                result.position()
        );
    }
}
