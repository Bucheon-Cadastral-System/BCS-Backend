package com.is.bcs.adapter.in.auth;

import com.is.bcs.application.port.in.auth.RefreshAccessTokenUseCase;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class RefreshTokenController {

    private final RefreshAccessTokenUseCase
            refreshAccessTokenUseCase;

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

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, Instant expiresAt) {
        Duration maxAge = Duration.between(Instant.now(), expiresAt);

        ResponseCookie cookie = ResponseCookie
                .from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/auth/token/refresh")
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