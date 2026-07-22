package com.is.bcs.application.port.out.token;

import com.is.bcs.domain.token.RefreshToken;

import java.util.Optional;

public interface RefreshTokenStore {

    void save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenId(String tokenId);

    void deleteByTokenId(String tokenId);

}