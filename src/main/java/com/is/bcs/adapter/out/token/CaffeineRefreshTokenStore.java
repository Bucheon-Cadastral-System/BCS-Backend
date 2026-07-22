package com.is.bcs.adapter.out.token;

import com.github.benmanes.caffeine.cache.Cache;
import com.is.bcs.application.port.out.token.RefreshTokenStore;
import com.is.bcs.domain.token.RefreshToken;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CaffeineRefreshTokenStore implements RefreshTokenStore {

    private final Cache<String, RefreshToken> refreshTokenCache;

    public CaffeineRefreshTokenStore(@Qualifier("refreshTokenCache") Cache<String, RefreshToken> refreshTokenCache) {
        this.refreshTokenCache = refreshTokenCache;
    }

    @Override
    public void save(RefreshToken refreshToken) {
        refreshTokenCache.put(
                refreshToken.tokenId(),
                refreshToken
        );
    }

    @Override
    public Optional<RefreshToken> findByTokenId(String tokenId) {
        return Optional.ofNullable(
                refreshTokenCache.getIfPresent(tokenId)
        );
    }

    @Override
    public void deleteByTokenId(String tokenId) {
        refreshTokenCache.invalidate(tokenId);
    }

}