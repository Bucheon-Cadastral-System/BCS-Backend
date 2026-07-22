package com.is.bcs.application.port.out.token;

import java.time.Instant;

public record IssuedTokenPair(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        String refreshTokenId,
        Instant refreshTokenExpiresAt
) {
}