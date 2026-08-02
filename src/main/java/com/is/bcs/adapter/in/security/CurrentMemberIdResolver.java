package com.is.bcs.adapter.in.security;

import com.is.bcs.adapter.in.security.oauth2.BcsOAuth2Principal;
import com.is.bcs.application.port.out.token.AccessTokenClaims;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class CurrentMemberIdResolver {

    public Long resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof BcsOAuth2Principal oauthPrincipal) {
            return oauthPrincipal.getMemberId();
        }

        if (principal instanceof AccessTokenClaims claims) {
            return claims.memberId();
        }

        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException("인증 사용자 정보가 없습니다.");
        }

        throw new AuthenticationCredentialsNotFoundException("지원하지 않는 인증 사용자 정보입니다.");
    }
}