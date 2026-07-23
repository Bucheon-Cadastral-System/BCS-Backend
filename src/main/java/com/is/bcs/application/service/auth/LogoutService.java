package com.is.bcs.application.service.auth;

import com.is.bcs.application.port.in.auth.LogoutUseCase;
import com.is.bcs.application.port.out.token.RefreshTokenClaims;
import com.is.bcs.application.port.out.token.RefreshTokenStore;
import com.is.bcs.application.port.out.token.TokenHasher;
import com.is.bcs.application.port.out.token.TokenProvider;
import com.is.bcs.domain.token.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

    private final TokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenHasher tokenHasher;

    @Override
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        RefreshTokenClaims claims = tokenProvider.validateRefreshToken(rawRefreshToken);

        RefreshToken storedToken =
                refreshTokenStore
                        .getAndDelete(claims.tokenId())
                        .orElse(null);

        if (storedToken == null) {
            return;
        }

        if (!storedToken.memberId().equals(claims.memberId())) {
            throw new IllegalArgumentException("Refresh Token의 사용자 정보가 일치하지 않습니다.");
        }

        String presentedHash = tokenHasher.hash(rawRefreshToken);

        if (!storedToken.tokenHash().equals(presentedHash)) {
            throw new IllegalArgumentException("Refresh Token이 일치하지 않습니다.");
        }
    }
}