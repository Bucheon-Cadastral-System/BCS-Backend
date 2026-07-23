package com.is.bcs.adapter.in.auth;

import com.is.bcs.application.port.in.auth.LogoutUseCase;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class LogoutController {

    private final LogoutUseCase logoutUseCase;

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

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie
                .from("refresh_token", "")
                .httpOnly(true)
                .secure(false) // 로컬
                .sameSite("Lax")
                .path("/api/auth/token/refresh")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }
}