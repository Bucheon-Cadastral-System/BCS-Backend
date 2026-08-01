package com.is.bcs.application.port.out.token;

import com.is.bcs.domain.token.OAuthExchangeGrant;

import java.util.Optional;

public interface OAuthCodeStore {

    void save(String code, OAuthExchangeGrant grant);

    Optional<OAuthExchangeGrant> getAndDelete(String code);
}