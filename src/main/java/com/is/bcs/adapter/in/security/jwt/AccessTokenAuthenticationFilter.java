package com.is.bcs.adapter.in.security.jwt;

import com.is.bcs.application.port.out.token.AccessTokenClaims;
import com.is.bcs.application.port.out.token.TokenProvider;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AccessTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final TokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
        ) throws ServletException, IOException {

        String accessToken = resolveAccessToken(request); // "Bearer " 제거한 토큰 꺼내기

        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AccessTokenClaims claims = tokenProvider.validateAccessToken(accessToken);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            claims,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name()))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication); // Role 등록


        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {
        String path = request.getServletPath();

        return path.equals("/api/auth/token/exchange")
                || path.equals("/api/auth/token/refresh")
                || path.equals("/api/auth/logout")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/item/v3/api-docs");
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = authorization.substring(BEARER_PREFIX.length());

        return token.isBlank() ? null : token;
    }
}