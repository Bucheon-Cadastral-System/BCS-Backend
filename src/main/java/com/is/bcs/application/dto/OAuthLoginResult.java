package com.is.bcs.application.dto;

import com.is.bcs.domain.member.MemberRole;
import com.is.bcs.domain.member.MemberStatus;

public record OAuthLoginResult(
        Long memberId,
        MemberRole role,
        MemberStatus status,
        boolean profileCompleted
) {
}