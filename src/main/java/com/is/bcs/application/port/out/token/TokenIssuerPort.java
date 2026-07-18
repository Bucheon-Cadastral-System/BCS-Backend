package com.is.bcs.application.port.out.token;

import com.is.bcs.domain.member.MemberRole;

public interface TokenIssuerPort {

    String issueAccessToken(Long memberId, MemberRole role);

    String issueRefreshToken(Long memberId);
}