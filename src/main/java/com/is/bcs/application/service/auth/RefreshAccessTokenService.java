package com.is.bcs.application.service.auth;

import com.is.bcs.application.port.in.auth.RefreshAccessTokenUseCase;
import com.is.bcs.application.port.out.member.LoadMemberPort;
import com.is.bcs.application.port.out.token.*;
import com.is.bcs.domain.member.Member;
import com.is.bcs.domain.member.MemberStatus;
import com.is.bcs.domain.token.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshAccessTokenService implements RefreshAccessTokenUseCase {

    private final TokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenHasher tokenHasher;
    private final LoadMemberPort loadMemberPort;

    @Override
    public RefreshTokenResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh Token이 없습니다.");
        }

        // JWT 서명, 만료 시각, REFRESH 타입 검증
        RefreshTokenClaims claims = tokenProvider.validateRefreshToken(rawRefreshToken);

        // 캐시에서 기존 토큰을 원자적으로 꺼내면서 폐기
        RefreshToken storedToken = refreshTokenStore
                .getAndDelete(claims.tokenId())
                .orElseThrow(() -> new IllegalArgumentException("폐기되었거나 유효하지 않은 Refresh Token입니다."));

        // JWT subject와 저장된 회원 검증
        if (!storedToken.memberId().equals(claims.memberId())) {
            throw new IllegalArgumentException("Refresh Token의 사용자 정보가 일치하지 않습니다.");
        }

        // 쿠키의 원문 토큰과 캐시에 저장된 해시 검증
        String presentedTokenHash = tokenHasher.hash(rawRefreshToken);

        if (!storedToken.tokenHash().equals(presentedTokenHash)) {
            throw new IllegalArgumentException("Refresh Token이 일치하지 않습니다.");
        }

        // 최신 회원 상태 및 권한 조회
        Member member = loadMemberPort.findById(claims.memberId())
                        .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new IllegalArgumentException("활성 회원이 아닙니다.");
        }

        // 새로운 Access/Refresh Token 쌍 발급
        IssuedTokenPair issuedTokens = tokenProvider.issue(
                        member.getId(),
                        member.getRole());

        // 새로운 Refresh Token 해시 저장
        RefreshToken newStoredToken =
                new RefreshToken(
                        issuedTokens.refreshTokenId(),
                        member.getId(),
                        tokenHasher.hash(issuedTokens.refreshToken()),
                        issuedTokens.refreshTokenExpiresAt()
                );

        refreshTokenStore.save(newStoredToken);

        return new RefreshTokenResult(
                issuedTokens.accessToken(),
                issuedTokens.accessTokenExpiresAt(),
                issuedTokens.refreshToken(),
                issuedTokens.refreshTokenExpiresAt()
        );
    }
}