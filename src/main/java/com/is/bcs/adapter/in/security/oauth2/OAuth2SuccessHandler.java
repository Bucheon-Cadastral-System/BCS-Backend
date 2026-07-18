package com.is.bcs.adapter.in.security.oauth2;

import com.is.bcs.domain.member.MemberStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler
        implements AuthenticationSuccessHandler {

    private final String frontendBaseUrl;

    public OAuth2SuccessHandler(
            @Value("${app.frontend-base-url}")
            String frontendBaseUrl
    ) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        BcsOAuth2Principal principal =
                getPrincipal(authentication);

        MemberStatus status = principal.getStatus();

        switch (status) {
            case PENDING -> handlePending(response);
            case ACTIVE -> handleActive(response);
            case INACTIVE -> handleInactive(request, response);
        }
    }

    private BcsOAuth2Principal getPrincipal(
            Authentication authentication
    ) {
        Object principal = authentication.getPrincipal();

        if (!(principal instanceof BcsOAuth2Principal bcsPrincipal)) {
            throw new IllegalStateException(
                    "지원하지 않는 인증 사용자입니다."
            );
        }

        return bcsPrincipal;
    }

    private void handlePending(
            HttpServletResponse response
    ) throws IOException {
        response.sendRedirect(
                frontendBaseUrl + "/signup"
        );
    }

    private void handleActive(
            HttpServletResponse response
    ) throws IOException {
        // 다음 단계에서 자체 Access/Refresh Token을 발급한다.
        response.sendRedirect(
                frontendBaseUrl + "/oauth/success"
        );
    }

    private void handleInactive(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        response.sendRedirect(
                frontendBaseUrl + "/login?error=inactive"
        );
    }
}