package com.is.bcs.application.port.in.auth;

import java.time.Instant;

public interface RefreshAccessTokenUseCase {

    RefreshTokenResult refresh(String refreshToken);

    record RefreshTokenResult(
            String accessToken,
            Instant accessTokenExpiresAt,
            String refreshToken,
            Instant refreshTokenExpiresAt
    ) {
    }
}