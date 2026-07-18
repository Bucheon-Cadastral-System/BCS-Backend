package com.is.bcs.application.dto;

import com.is.bcs.domain.member.OAuthProvider;

public record OAuthLoginCommand(
        OAuthProvider provider,
        String providerUserId
) {
}