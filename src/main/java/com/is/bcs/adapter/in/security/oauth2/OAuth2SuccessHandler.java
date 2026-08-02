package com.is.bcs.adapter.in.security.oauth2;

import com.is.bcs.adapter.in.security.oauth2.exception.InvalidOAuth2PrincipalException;
import com.is.bcs.application.port.in.auth.CompleteOAuth2LoginUseCase;
import com.is.bcs.domain.member.MemberStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final String frontendBaseUrl;
    private final CompleteOAuth2LoginUseCase completeOAuth2LoginUseCase;
    private final OAuth2LoginFailureHandler oauth2LoginFailureHandler;

    public OAuth2SuccessHandler(
            @Value("${app.frontend-base-url}")
            String frontendBaseUrl,
            CompleteOAuth2LoginUseCase completeOAuth2LoginUseCase,
            OAuth2LoginFailureHandler oauth2LoginFailureHandler
    ) {
        this.frontendBaseUrl = frontendBaseUrl;
        this.completeOAuth2LoginUseCase = completeOAuth2LoginUseCase;
        this.oauth2LoginFailureHandler = oauth2LoginFailureHandler;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        final BcsOAuth2Principal principal;

        try {
            principal = getPrincipal(authentication);
        } catch (InvalidOAuth2PrincipalException e) {
            oauth2LoginFailureHandler.onAuthenticationFailure(
                    request,
                    response,
                    e
            );
            return;
        }

        MemberStatus status = principal.getStatus();

        switch (status) {
            case PENDING -> handlePending(response);
            case ACTIVE -> handleActive(request, response, principal);
            case INACTIVE -> handleInactive(request, response);
        }
    }

    private BcsOAuth2Principal getPrincipal(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (!(principal instanceof BcsOAuth2Principal bcsPrincipal)) {
            throw new InvalidOAuth2PrincipalException("지원하지 않는 OAuth2 인증 사용자입니다.");
        }

        return bcsPrincipal;
    }

    private void handlePending(HttpServletResponse response) throws IOException {
        response.sendRedirect(
                frontendBaseUrl + "/signup"
        );
    }

    private void handleActive(HttpServletRequest request, HttpServletResponse response, BcsOAuth2Principal principal) throws IOException {

        // 1. 객체 변환
        CompleteOAuth2LoginUseCase.Command command =
                new CompleteOAuth2LoginUseCase.Command(
                        principal.getMemberId(),
                        getCodeChallenge(request)
                );

        // 2. 액세스 토큰 & 리프레시 토큰 발행 후 <일회용 코드, OAuthExchangeToken> 캐시 저장
        CompleteOAuth2LoginUseCase.Result result = completeOAuth2LoginUseCase.complete(command);

        // 3. HttpSession Clear
        clearAuthenticationSession(request);

        // 4. 일회용 코드 Encode
        String encodedCode = URLEncoder.encode(result.exchangeCode(), StandardCharsets.UTF_8);

        // 5. 리다이 렉트 (일회용 코드 포함)
        response.sendRedirect(
                frontendBaseUrl
                        + "/oauth/success?code="
                        + encodedCode
        );

    }

    private void handleInactive(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        response.sendRedirect(
                frontendBaseUrl + "/login?error=inactive"
        );
    }


    private void clearAuthenticationSession(
            HttpServletRequest request
    ) {
        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }
    }

    private String getCodeChallenge(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new InvalidOAuth2PrincipalException("OAuth 로그인 세션이 없습니다.");
        }

        Object value = session.getAttribute(OAuth2SessionAttributes.CODE_CHALLENGE);

        if (!(value instanceof String codeChallenge) || codeChallenge.isBlank()) {
            throw new InvalidOAuth2PrincipalException("코드 챌린지가 없습니다.");
        }

        return codeChallenge;
    }



}