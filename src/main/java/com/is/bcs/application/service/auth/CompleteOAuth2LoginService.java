package com.is.bcs.application.service.auth;

import com.is.bcs.application.port.in.auth.CompleteOAuth2LoginUseCase;
import com.is.bcs.application.port.out.token.OAuthCodeStore;
import com.is.bcs.config.properties.OAuthProperties;
import com.is.bcs.domain.token.OAuthExchangeGrant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompleteOAuth2LoginService
        implements CompleteOAuth2LoginUseCase {

    private final OAuthCodeStore oauthCodeStore;
    private final OAuthProperties oauthProperties;
    private final Clock clock;

    @Override
    public Result complete(Command command) {
        String exchangeCode = UUID.randomUUID().toString();

        OAuthExchangeGrant grant = new OAuthExchangeGrant(
                command.memberId(),
                command.codeChallenge(),
                clock.instant().plus(
                        oauthProperties.exchangeCodeTtl()
                )
        );

        oauthCodeStore.save(exchangeCode, grant);

        return new Result(exchangeCode);
    }
}