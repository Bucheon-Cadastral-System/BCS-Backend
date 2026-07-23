package com.is.bcs.adapter.out.security.jwt;

import com.is.bcs.application.port.out.token.AccessTokenClaims;
import com.is.bcs.application.port.out.token.IssuedTokenPair;
import com.is.bcs.application.port.out.token.RefreshTokenClaims;
import com.is.bcs.application.port.out.token.TokenProvider;
import com.is.bcs.config.properties.JwtProperties;
import com.is.bcs.domain.member.MemberRole;
import com.is.bcs.domain.token.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider implements TokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ROLE_CLAIM = "role";

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey secretKey;
    private final JwtParser jwtParser;

    public JwtTokenProvider(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;

        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));

        this.jwtParser = Jwts.parser()
                .verifyWith(secretKey)
                .build();
    }

    @Override
    public IssuedTokenPair issue(Long memberId, MemberRole role) {
        Instant issuedAt = clock.instant();

        Instant accessTokenExpiresAt = issuedAt.plus(properties.accessTokenExpiration());

        Instant refreshTokenExpiresAt = issuedAt.plus(properties.refreshTokenExpiration());

        String refreshTokenId = UUID.randomUUID().toString();

        String accessToken = createAccessToken(
                memberId,
                role,
                issuedAt,
                accessTokenExpiresAt
        );

        String refreshToken = createRefreshToken(
                memberId,
                refreshTokenId,
                issuedAt,
                refreshTokenExpiresAt
        );

        return new IssuedTokenPair(
                accessToken,
                accessTokenExpiresAt,
                refreshToken,
                refreshTokenId,
                refreshTokenExpiresAt
        );
    }

    @Override
    public AccessTokenClaims validateAccessToken(String token) {
        Claims claims = parseClaims(token);

        validateTokenType(claims, TokenType.ACCESS);

        return new AccessTokenClaims(
                parseMemberId(claims),
                MemberRole.valueOf(claims.get(ROLE_CLAIM, String.class)),
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()
        );
    }

    @Override
    public RefreshTokenClaims validateRefreshToken(String token) {
        Claims claims = parseClaims(token);

        validateTokenType(claims, TokenType.REFRESH);

        return new RefreshTokenClaims(
                claims.getId(),
                parseMemberId(claims),
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()
        );
    }

    private String createAccessToken(
            Long memberId,
            MemberRole role,
            Instant issuedAt,
            Instant expiresAt
    ) {
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(ROLE_CLAIM, role.name())
                .claim(TOKEN_TYPE_CLAIM, TokenType.ACCESS.name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    private String createRefreshToken(
            Long memberId,
            String refreshTokenId,
            Instant issuedAt,
            Instant expiresAt
    ) {
        return Jwts.builder()
                .id(refreshTokenId)
                .subject(String.valueOf(memberId))
                .claim(TOKEN_TYPE_CLAIM, TokenType.REFRESH.name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("토큰이 비어 있습니다.");
        }

        return jwtParser
                .parseSignedClaims(token)
                .getPayload();
    }

    private void validateTokenType(Claims claims, TokenType expectedType) {
        String actualType = claims.get(
                TOKEN_TYPE_CLAIM,
                String.class
        );

        if (!expectedType.name().equals(actualType)) {
            throw new IllegalArgumentException("올바르지 않은 토큰 타입입니다.");
        }
    }

    private Long parseMemberId(Claims claims) {
        String subject = claims.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("토큰에 사용자 식별자가 없습니다.");
        }

        return Long.valueOf(subject);
    }
}