package com.is.bcs.application.port.in.auth;

import java.time.Instant;

public interface ExchangeOAuthCodeUseCase {

    ExchangeOAuthCodeResult exchange(String code);

    record ExchangeOAuthCodeResult(
            String accessToken,
            Instant accessTokenExpiresAt
    ) {
    }
}