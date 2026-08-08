package com.is.bcs.adapter.in.security.oauth2;

import com.is.bcs.adapter.in.security.oauth2.exception.InvalidOAuth2PrincipalException;
import com.is.bcs.adapter.in.security.oauth2.exception.InvalidOAuth2UserInfoException;
import com.is.bcs.adapter.in.security.oauth2.exception.UnsupportedOAuth2ProviderException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final String frontendBaseUrl;

    public OAuth2LoginFailureHandler(@Value("${app.frontend-base-url}") String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        String errorCode = resolveErrorCode(exception);

        log.warn(
                "OAuth2 login failed. errorCode={}, requestUri={}, exceptionType={}",
                errorCode,
                request.getRequestURI(),
                exception.getClass().getSimpleName()
        );

        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);

        if (session != null) {session.invalidate();}

        response.sendRedirect(frontendBaseUrl + "/login?error=" + errorCode);
    }

    private String resolveErrorCode(AuthenticationException exception) {
        if (exception instanceof InvalidOAuth2UserInfoException) {
            return "oauth2_user_info_invalid";
        }

        if (exception instanceof UnsupportedOAuth2ProviderException) {
            return "oauth2_provider_unsupported";
        }

        if (exception instanceof InvalidOAuth2PrincipalException) {
            return "oauth2_principal_invalid";
        }

        return "oauth2_authentication_failed";
    }
}