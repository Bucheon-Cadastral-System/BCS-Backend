package com.is.bcs.application.port.out.token;

import com.is.bcs.domain.token.OAuthExchangeToken;

import java.util.Optional;

public interface OAuthCodeStore {

    void save(String code, OAuthExchangeToken token);

    Optional<OAuthExchangeToken> getAndDelete(String code);

}