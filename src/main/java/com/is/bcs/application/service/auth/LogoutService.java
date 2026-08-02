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
            log.info("토큰이 비어있습니다.");
            return;
        }

        RefreshTokenClaims claims = tokenProvider.validateRefreshToken(rawRefreshToken);

        RefreshToken storedToken =
                refreshTokenStore
                        .getAndDelete(claims.tokenId())
                        .orElse(null);

        if (storedToken == null) {
            log.info("이미 로그아웃 처리된 RefreshToken입니다.");
            return;
        }

        if (!storedToken.memberId().equals(claims.memberId())) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }


        if (!tokenHasher.matches(rawRefreshToken, storedToken.tokenHash())) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }

        log.info("Logout successful.");
    }
}