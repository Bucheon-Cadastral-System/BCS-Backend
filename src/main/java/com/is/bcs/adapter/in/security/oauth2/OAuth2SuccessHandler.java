package com.is.bcs.adapter.in.security.oauth2;

import com.is.bcs.adapter.in.security.oauth2.exception.InvalidOAuth2PrincipalException;
import com.is.bcs.application.port.out.token.*;
import com.is.bcs.domain.member.MemberStatus;
import com.is.bcs.domain.token.OAuthExchangeToken;
import com.is.bcs.domain.token.RefreshToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final String frontendBaseUrl;
    private final boolean refreshCookieSecure;
    private final TokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenHasher tokenHasher;
    private final OAuthCodeStore oauthCodeStore;
    private final Clock clock;
    private final OAuth2LoginFailureHandler oauth2LoginFailureHandler;

    public OAuth2SuccessHandler(
            @Value("${app.frontend-base-url}")
            String frontendBaseUrl,
            @Value("${app.auth.refresh-cookie.secure}")
            boolean refreshCookieSecure,
            TokenProvider tokenProvider,
            RefreshTokenStore refreshTokenStore,
            TokenHasher tokenHasher,
            OAuthCodeStore oauthCodeStore,
            Clock clock,
            OAuth2LoginFailureHandler oauth2LoginFailureHandler
    ) {
        this.frontendBaseUrl = frontendBaseUrl;
        this.refreshCookieSecure = refreshCookieSecure;
        this.tokenProvider = tokenProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.tokenHasher = tokenHasher;
        this.oauthCodeStore = oauthCodeStore;
        this.clock = clock;
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

        // 1. Access/Refresh Token 발급
        IssuedTokenPair issuedTokens = tokenProvider.issue(
                principal.getMemberId(),
                principal.getRole()
        );

        // 2. Refresh Token 해시와 메타데이터 저장
        RefreshToken refreshToken = new RefreshToken(
                issuedTokens.refreshTokenId(),
                principal.getMemberId(),
                tokenHasher.hash(
                        issuedTokens.refreshToken()
                ),
                issuedTokens.refreshTokenExpiresAt()
        );

        refreshTokenStore.save(refreshToken);

        // 3. Refresh Token 원문을 HttpOnly 쿠키로 전달
        addRefreshTokenCookie(
                response,
                issuedTokens.refreshToken(),
                issuedTokens.refreshTokenExpiresAt()
        );

        // 4. Access Token을 일회용 코드에 연결해 임시 저장
        String exchangeCode = UUID.randomUUID().toString();

        OAuthExchangeToken exchangeToken =
                new OAuthExchangeToken(
                        issuedTokens.accessToken(),
                        issuedTokens.accessTokenExpiresAt(),
                        clock.instant().plusSeconds(60)
                );

        oauthCodeStore.save(
                exchangeCode,
                exchangeToken
        );


        // 5. 기존 OAuth 세션 제거
        clearAuthenticationSession(request);

        // 6. 프론트로 일회용 코드 전달
        String encodedCode = URLEncoder.encode(
                exchangeCode,
                StandardCharsets.UTF_8
        );

        response.sendRedirect(
                frontendBaseUrl
                        + "/oauth/success?code="
                        + encodedCode
        );

        /** 개발용 일회용코드, AccessToken, RefreshToken 출력 */
        log.info("일회용 코드 : {}", exchangeCode);
        log.info("액세스 토큰 : {}", issuedTokens.accessToken());
        log.info("리프레시 토큰 : {}", issuedTokens.refreshToken());

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

    private void addRefreshTokenCookie(
            HttpServletResponse response,
            String refreshToken,
            Instant expiresAt
    ) {
        Duration maxAge = Duration.between(
                clock.instant(),
                expiresAt
        );

        ResponseCookie cookie = ResponseCookie
                .from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
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



}