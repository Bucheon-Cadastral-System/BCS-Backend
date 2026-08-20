package com.is.bcs.adapter.in.member;

import com.is.bcs.application.port.in.member.GetMyProfileUseCase;
import com.is.bcs.domain.member.District;
import com.is.bcs.domain.member.Position;
import com.is.bcs.domain.member.Team;

/**
 * 다른 회원의 신원 — 화면이 이름 옆에서 그 사람이 누구인지 보일 때 쓴다.
 *
 * <p>연락처(전화번호·이메일)와 권한은 싣지 않는다. 인증만 하면 누구나 회원 번호를 바꿔 가며 물을 수 있는
 * 경로라, 여기에 실린 값은 전 직원분이 그대로 열리는 것과 같다. 그 값들은 관리자 화면이 다룬다.
 *
 * <p>가입 상태도 싣지 않는다. 승인·비활성은 관리자 화면이 다루는 값이고, 이름을 눌러 본 사람이 알 값이 아니다.
 */
public record MemberSummaryResponse(
        Long id,
        String name,
        District district,
        String department,
        Team team,
        Position position
) {

    public static MemberSummaryResponse from(GetMyProfileUseCase.Result result) {
        return new MemberSummaryResponse(
                result.id(),
                result.name(),
                result.district(),
                result.department(),
                result.team(),
                result.position()
        );
    }
}
