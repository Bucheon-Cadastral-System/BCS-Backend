package com.is.bcs.adapter.out.token;

import com.github.benmanes.caffeine.cache.Cache;
import com.is.bcs.application.port.out.token.OAuthCodeStore;
import com.is.bcs.domain.token.OAuthExchangeGrant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CaffeineOAuthCodeStore implements OAuthCodeStore {

    private final Cache<String, OAuthExchangeGrant> oauthCodeCache;

    public CaffeineOAuthCodeStore(
            @Qualifier("oauthCodeCache") Cache<String, OAuthExchangeGrant> oauthCodeCache) {
        this.oauthCodeCache = oauthCodeCache;
    }

    @Override
    public void save(String code, OAuthExchangeGrant grant) {
        oauthCodeCache.put(code, grant);
    }

    @Override
    public Optional<OAuthExchangeGrant> getAndDelete(String code) {
        OAuthExchangeGrant grant = oauthCodeCache.asMap().remove(code);
        return Optional.ofNullable(grant);
    }
}