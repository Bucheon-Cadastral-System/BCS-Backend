package com.is.bcs.application.service.auth;

import com.is.bcs.application.port.in.auth.ExchangeOAuthCodeUseCase;
import com.is.bcs.application.port.out.member.LoadMemberPort;
import com.is.bcs.application.port.out.token.IssuedTokenPair;
import com.is.bcs.application.port.out.token.OAuthCodeStore;
import com.is.bcs.application.port.out.token.RefreshTokenStore;
import com.is.bcs.application.port.out.token.TokenHasher;
import com.is.bcs.application.port.out.token.TokenProvider;
import com.is.bcs.domain.member.Member;
import com.is.bcs.domain.member.MemberStatus;
import com.is.bcs.domain.token.OAuthExchangeGrant;
import com.is.bcs.domain.token.RefreshToken;
import com.is.bcs.domain.token.exception.ExpiredOAuthExchangeCodeException;
import com.is.bcs.domain.token.exception.InvalidOAuthExchangeCodeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ExchangeOAuthCodeService implements ExchangeOAuthCodeUseCase {

    private final OAuthCodeStore oauthCodeStore;
    private final LoadMemberPort loadMemberPort;
    private final TokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenHasher tokenHasher;
    private final Clock clock;

    @Override
    public ExchangeOAuthCodeResult exchange(String code, String codeVerifier) {
        validateRequest(code, codeVerifier);

        OAuthExchangeGrant exchangeGrant = oauthCodeStore
                .getAndDelete(code)
                .orElseThrow(() -> new InvalidOAuthExchangeCodeException("유효하지 않은 일회용 코드입니다."));

        validateExpiration(exchangeGrant);
        validateCodeVerifier(exchangeGrant, codeVerifier);

        Member member = loadActiveMember(exchangeGrant.memberId());

        IssuedTokenPair issuedTokens = tokenProvider.issue(
                member.getId(),
                member.getRole()
        );

        saveRefreshToken(member.getId(), issuedTokens);

        return new ExchangeOAuthCodeResult(
                issuedTokens.accessToken(),
                issuedTokens.accessTokenExpiresAt(),
                issuedTokens.refreshToken(),
                issuedTokens.refreshTokenExpiresAt()
        );
    }

    private void validateRequest(String code, String codeVerifier) {
        if (code == null || code.isBlank()) {
            throw new InvalidOAuthExchangeCodeException("일회용 코드가 비어 있습니다.");
        }

        if (codeVerifier == null || codeVerifier.isBlank()) {
            throw new InvalidOAuthExchangeCodeException("코드 verifier가 비어 있습니다.");
        }
    }

    private void validateExpiration(OAuthExchangeGrant exchangeGrant) {
        if (!exchangeGrant.expiresAt().isAfter(clock.instant())) {
            throw new ExpiredOAuthExchangeCodeException("만료된 일회용 코드입니다.");
        }
    }

    private void validateCodeVerifier(OAuthExchangeGrant exchangeGrant, String codeVerifier) {
        String actualChallenge = createCodeChallenge(codeVerifier);

        if (!exchangeGrant.codeChallenge().equals(actualChallenge)) {
            throw new InvalidOAuthExchangeCodeException("코드 verifier가 올바르지 않습니다.");
        }
    }

    private Member loadActiveMember(Long memberId) {
        Member member = loadMemberPort.findById(memberId)
                .orElseThrow(() -> new InvalidOAuthExchangeCodeException("유효하지 않은 회원입니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new InvalidOAuthExchangeCodeException("활성 상태의 회원이 아닙니다.");
        }

        return member;
    }

    private void saveRefreshToken(Long memberId, IssuedTokenPair issuedTokens) {
        RefreshToken refreshToken = new RefreshToken(
                issuedTokens.refreshTokenId(),
                memberId,
                tokenHasher.hash(issuedTokens.refreshToken()),
                issuedTokens.refreshTokenExpiresAt()
        );

        refreshTokenStore.save(refreshToken);
    }

    private String createCodeChallenge(String codeVerifier) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] digest = messageDigest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(digest);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}