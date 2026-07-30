package com.is.bcs.adapter.out.token;

import com.github.benmanes.caffeine.cache.Cache;
import com.is.bcs.application.port.out.token.OAuthCodeStore;
import com.is.bcs.domain.token.OAuthExchangeToken;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CaffeineOAuthCodeStore implements OAuthCodeStore {

    private final Cache<String, OAuthExchangeToken> oauthCodeCache;

    public CaffeineOAuthCodeStore(@Qualifier("oauthCodeCache") Cache<String, OAuthExchangeToken> oauthCodeCache) {
        this.oauthCodeCache = oauthCodeCache;
    }

    @Override
    public void save(String code, OAuthExchangeToken exchangeToken) {

        oauthCodeCache.put(code, exchangeToken);
    }

    @Override
    public Optional<OAuthExchangeToken> getAndDelete(String code) {

        return Optional.ofNullable(
                oauthCodeCache.asMap().remove(code)
        );
    }

}