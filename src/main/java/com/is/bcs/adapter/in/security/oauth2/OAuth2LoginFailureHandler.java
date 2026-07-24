package com.is.bcs.adapter.in.security.oauth2;

import com.is.bcs.adapter.in.security.oauth2.exception.InvalidOAuth2PrincipalException;
import com.is.bcs.adapter.in.security.oauth2.exception.UnsupportedOAuth2ProviderException;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;
import com.is.bcs.adapter.in.security.oauth2.exception.InvalidOAuth2UserInfoException;
import com.is.bcs.adapter.in.web.exception.ErrorCode;
import com.is.bcs.adapter.in.web.exception.SecurityErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        ErrorCode errorCode;
        String detail;

        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        if (exception instanceof InvalidOAuth2UserInfoException) {
            errorCode = SecurityErrorCode.OAUTH2_USER_INFO_INVALID;
            detail = exception.getMessage();

        } else if (exception instanceof UnsupportedOAuth2ProviderException) {
            errorCode = SecurityErrorCode.OAUTH2_PROVIDER_UNSUPPORTED;
            detail = "지원하지 않는 OAuth2 제공자입니다.";

        }else if (exception instanceof InvalidOAuth2PrincipalException) {
            errorCode = SecurityErrorCode.OAUTH2_PRINCIPAL_INVALID;
            detail = "OAuth2 로그인 처리 중 오류가 발생했습니다.";

        } else {
            errorCode = SecurityErrorCode.OAUTH2_AUTHENTICATION_FAILED;
            detail = "OAuth2 인증에 실패했습니다.";
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.status(), detail);

        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", errorCode.code());
        problem.setProperty(
                "timestamp",
                OffsetDateTime.now(clock)
        );

        response.setStatus(errorCode.status().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}