package com.is.bcs.adapter.in.security.oauth2.exception;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public class InvalidOAuth2UserInfoException extends OAuth2AuthenticationException {

    private static final String OAUTH2_ERROR_CODE = "invalid_oauth2_user_info";

    public InvalidOAuth2UserInfoException(String message) {
        super(new OAuth2Error(OAUTH2_ERROR_CODE), message);
    }

}