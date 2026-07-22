package com.is.bcs.application.port.out.token;

import com.is.bcs.domain.member.MemberRole;

public interface TokenProvider {

    IssuedTokenPair issue(
            Long memberId,
            MemberRole role
    );

    AccessTokenClaims validateAccessToken(String token);

    RefreshTokenClaims validateRefreshToken(String token);
}