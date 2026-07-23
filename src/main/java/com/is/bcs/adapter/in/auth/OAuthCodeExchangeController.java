package com.is.bcs.adapter.in.auth;

import com.is.bcs.application.port.in.auth.ExchangeOAuthCodeUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class OAuthCodeExchangeController {

    private final ExchangeOAuthCodeUseCase exchangeOAuthCodeUseCase;

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

    public record OAuthCodeExchangeRequest(@NotBlank String code) {

    }

    public record OAuthCodeExchangeResponse(
            String accessToken,
            String tokenType,
            Instant expiresAt
    ) {
    }
}