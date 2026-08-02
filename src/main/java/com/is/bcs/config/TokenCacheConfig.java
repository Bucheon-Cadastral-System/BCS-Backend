package com.is.bcs.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.is.bcs.config.properties.JwtProperties;
import com.is.bcs.config.properties.OAuthProperties;
import com.is.bcs.domain.token.OAuthExchangeGrant;
import com.is.bcs.domain.token.RefreshToken;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenCacheConfig {

    @Bean("refreshTokenCache")
    public Cache<String, RefreshToken> refreshTokenCache(JwtProperties jwtProperties) {
        return Caffeine.newBuilder()
                .expireAfterWrite(
                    jwtProperties.refreshTokenExpiration()
                )
                .build();
    }

    @Bean("oauthCodeCache")
    public Cache<String, OAuthExchangeGrant> oauthCodeCache(OAuthProperties oauthProperties) {
        return Caffeine.newBuilder()
                .expireAfterWrite(oauthProperties.exchangeCodeTtl())
                .build();
    }

}