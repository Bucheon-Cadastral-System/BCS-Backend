package com.is.bcs.application.service.auth;

import com.is.bcs.application.port.in.auth.LogoutUseCase;
import com.is.bcs.application.port.out.token.RefreshTokenClaims;
import com.is.bcs.application.port.out.token.RefreshTokenStore;
import com.is.bcs.application.port.out.token.TokenHasher;
import com.is.bcs.application.port.out.token.TokenProvider;
import com.is.bcs.domain.token.RefreshToken;
import com.is.bcs.domain.token.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
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
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }

        String presentedHash = tokenHasher.hash(rawRefreshToken);

        if (!storedToken.tokenHash().equals(presentedHash)) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }
        log.info("Logout successful.");
    }
}