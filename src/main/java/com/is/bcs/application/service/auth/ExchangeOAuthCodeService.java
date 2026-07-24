package com.is.bcs.application.service.auth;

import com.is.bcs.application.port.in.auth.ExchangeOAuthCodeUseCase;
import com.is.bcs.application.port.out.token.OAuthCodeStore;
import com.is.bcs.domain.token.OAuthExchangeToken;
import com.is.bcs.domain.token.exception.ExpiredOAuthExchangeCodeException;
import com.is.bcs.domain.token.exception.InvalidOAuthExchangeCodeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class ExchangeOAuthCodeService implements ExchangeOAuthCodeUseCase {

    private final OAuthCodeStore oauthCodeStore;
    private final Clock clock;

    @Override
    public ExchangeOAuthCodeResult exchange(String code) {

        if (code == null || code.isBlank()) {
            throw new InvalidOAuthExchangeCodeException("일회용 코드가 비어 있습니다.");
        }

        OAuthExchangeToken exchangeToken =
                oauthCodeStore.getAndDelete(code)
                        .orElseThrow(() -> new InvalidOAuthExchangeCodeException("유효하지 않은 일회용 코드입니다."));

        if (exchangeToken.isExpired(clock.instant())) {
            throw new ExpiredOAuthExchangeCodeException("만료된 일회용 코드입니다.");
        }

        return new ExchangeOAuthCodeResult(
                exchangeToken.accessToken(),
                exchangeToken.accessTokenExpiresAt()
        );
    }
}