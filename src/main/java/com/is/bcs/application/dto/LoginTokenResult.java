package com.is.bcs.application.dto;

public record LoginTokenResult(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn
) {
}
