package com.is.bcs.adapter.in.security.oauth2.exception;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public class UnsupportedOAuth2ProviderException extends OAuth2AuthenticationException {

    private static final String ERROR_CODE = "unsupported_oauth2_provider";

    public UnsupportedOAuth2ProviderException(String message) {
        super(new OAuth2Error(ERROR_CODE), message);
    }
}