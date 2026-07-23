package com.is.bcs.domain.token;

import com.is.bcs.domain.member.MemberRole;

import java.time.Instant;

public record AccessToken(
        Long memberId,
        MemberRole role,
        Instant issuedAt,
        Instant expiresAt
) {

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}