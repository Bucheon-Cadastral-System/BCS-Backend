package com.is.bcs.application.port.in.oauth;

import com.is.bcs.domain.member.OAuthProvider;

public record OAuthLoginCommand(
        OAuthProvider provider, // KAKAO 고정
        String providerUserId
) {
}