package com.is.bcs.application.port.in.auth;

import java.time.Instant;

public interface ExchangeOAuthCodeUseCase {

    ExchangeOAuthCodeResult exchange(String code, String codeVerifier);

    record ExchangeOAuthCodeResult(
            String accessToken,
            Instant accessTokenExpiresAt,
            String refreshToken,
            Instant refreshTokenExpiresAt
    ) {
    }

}