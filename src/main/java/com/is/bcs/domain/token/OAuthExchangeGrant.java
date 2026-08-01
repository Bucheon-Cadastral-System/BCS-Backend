package com.is.bcs.domain.token;

import java.time.Instant;

public record OAuthExchangeGrant(
        Long memberId,
        String codeChallenge,
        Instant expiresAt
) {
}