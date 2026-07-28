package com.is.bcs.adapter.in.auth;

import com.is.bcs.application.port.in.auth.ExchangeOAuthCodeUseCase;
import com.is.bcs.application.port.in.auth.LogoutUseCase;
import com.is.bcs.application.port.in.auth.RefreshAccessTokenUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;


@Tag(name = "1. AuthController", description = "로그인, 로그아웃, 토큰 갱신")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final boolean refreshCookieSecure;
    private final LogoutUseCase logoutUseCase;
    private final ExchangeOAuthCodeUseCase exchangeOAuthCodeUseCase;
    private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;

    public AuthController(
            @Value("${app.auth.refresh-cookie.secure}")
            boolean refreshCookieSecure,
            LogoutUseCase logoutUseCase,
            ExchangeOAuthCodeUseCase exchangeOAuthCodeUseCase,
            RefreshAccessTokenUseCase refreshAccessTokenUseCase) {
        this.refreshCookieSecure = refreshCookieSecure;
        this.logoutUseCase = logoutUseCase;
        this.exchangeOAuthCodeUseCase = exchangeOAuthCodeUseCase;
        this.refreshAccessTokenUseCase = refreshAccessTokenUseCase;
    }

    @Operation(summary = "일회용 임시 코드로 액세스 토큰(바디) & 리프레시 토큰 교환(쿠키)")
    @PostMapping("/token/exchange")
    public ResponseEntity<OAuthCodeExchangeResponse> exchange(@Valid @RequestBody OAuthCodeExchangeRequest request) {
        ExchangeOAuthCodeUseCase.ExchangeOAuthCodeResult result = exchangeOAuthCodeUseCase.exchange(request.code());

        OAuthCodeExchangeResponse response =
                new OAuthCodeExchangeResponse(
                        result.accessToken(),
                        "Bearer",
                        result.accessTokenExpiresAt()
                );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @Operation(summary = "리프레시 토큰으로 로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false)
            String refreshToken,
            HttpServletResponse response
    ) {
        try {
            logoutUseCase.logout(refreshToken);
        } finally {
            deleteRefreshTokenCookie(response);
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "리프레시 토큰으로 토큰 갱신")
    @PostMapping("/token/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(
            @CookieValue(name = "refresh_token", required = false)
            String refreshToken,
            HttpServletResponse response
    ) {
        RefreshAccessTokenUseCase.RefreshTokenResult result = refreshAccessTokenUseCase.refresh(refreshToken);

        addRefreshTokenCookie(response, result.refreshToken(), result.refreshTokenExpiresAt());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        new RefreshTokenResponse(
                                result.accessToken(),
                                "Bearer",
                                result.accessTokenExpiresAt()
                        )
                );
    }


    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie
                .from("refresh_token", "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }

    public record OAuthCodeExchangeRequest(@NotBlank String code) {

    }

    public record OAuthCodeExchangeResponse(
            String accessToken,
            String tokenType,
            Instant expiresAt
    ) {
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, Instant expiresAt) {
        Duration maxAge = Duration.between(Instant.now(), expiresAt);

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

    public record RefreshTokenResponse(
            String accessToken,
            String tokenType,
            Instant expiresAt
    ) {
    }


}
