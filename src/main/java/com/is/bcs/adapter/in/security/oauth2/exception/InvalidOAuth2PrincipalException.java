package com.is.bcs.adapter.in.security.oauth2.exception;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public class InvalidOAuth2PrincipalException extends OAuth2AuthenticationException {

    private static final String ERROR_CODE =
            "invalid_oauth2_principal";

    public InvalidOAuth2PrincipalException(String message) {
        super(new OAuth2Error(ERROR_CODE), message);
    }
}