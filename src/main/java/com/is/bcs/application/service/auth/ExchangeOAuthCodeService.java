package com.is.bcs.application.service.auth;

import com.is.bcs.application.port.in.auth.ExchangeOAuthCodeUseCase;
import com.is.bcs.application.port.out.token.OAuthCodeStore;
import com.is.bcs.domain.token.OAuthExchangeToken;
import com.is.bcs.domain.token.exception.ExpiredOAuthExchangeCodeException;
import com.is.bcs.domain.token.exception.InvalidOAuthExchangeCodeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeOAuthCodeService implements ExchangeOAuthCodeUseCase {

    private final OAuthCodeStore oauthCodeStore;
    private final Clock clock;

    @Override
    public ExchangeOAuthCodeResult exchange(String code, String codeVerifier) {

        if (code == null || code.isBlank()) {
            throw new InvalidOAuthExchangeCodeException("일회용 코드가 비어 있습니다.");
        }

        if (codeVerifier == null || codeVerifier.isBlank()) {
            throw new InvalidOAuthExchangeCodeException("코드 verifier가 비어 있습니다.");
        }

        OAuthExchangeToken exchangeToken =
                oauthCodeStore.getAndDelete(code)
                        .orElseThrow(() -> new InvalidOAuthExchangeCodeException("유효하지 않은 일회용 코드입니다."));

        if (exchangeToken.isExpired(clock.instant())) {
            throw new ExpiredOAuthExchangeCodeException("만료된 일회용 코드입니다.");
        }

        String actualChallenge = createCodeChallenge(codeVerifier);

        if (!exchangeToken.codeChallenge().equals(actualChallenge)) {
            throw new InvalidOAuthExchangeCodeException("코드 verifier가 올바르지 않습니다.");
        }

        log.info("codeVerifier와 세션에서 꺼낸 codeChallenge가 같습니다.");


        return new ExchangeOAuthCodeResult(
                exchangeToken.accessToken(),
                exchangeToken.accessTokenExpiresAt()
        );
    }

    private String createCodeChallenge(String codeVerifier) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] digest = messageDigest
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(digest);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

}