package com.is.bcs.application.port.out.token;

import com.is.bcs.domain.member.MemberRole;

import java.time.Instant;

public record AccessTokenClaims(
        Long memberId,       // memberId (pk)
        MemberRole role,     // memberRole
        Instant issuedAt,
        Instant expiresAt
) {
}