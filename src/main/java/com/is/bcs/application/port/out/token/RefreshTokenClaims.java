package com.is.bcs.application.port.out.token;

import java.time.Instant;

public record RefreshTokenClaims(
        String tokenId,  // Token ID
        Long memberId,   // member ID (PK)
        Instant issuedAt,// 발행 시간
        Instant expiresAt// 만료 시간
) {
}