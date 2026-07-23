package com.is.bcs.domain.token;

import java.time.Instant;

public record OAuthExchangeToken(
        String accessToken,
        Instant accessTokenExpiresAt,
        Instant expiresAt
) {

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}