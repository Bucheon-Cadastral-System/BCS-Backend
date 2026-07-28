package com.is.bcs.adapter.out.security.jwt;

import com.is.bcs.application.port.out.token.TokenHasher;
import com.is.bcs.domain.token.exception.TokenHashingException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class Sha256TokenHasher implements TokenHasher {

    @Override
    public String hash(String token) {
        if (token == null || token.isBlank()) {
            throw new TokenHashingException("해싱할 토큰이 비어 있습니다.");
        }

        return HexFormat.of().formatHex(digest(token));
    }

    @Override
    public boolean matches(String rawToken, String tokenHash) {

        if (rawToken == null || rawToken.isBlank() || tokenHash == null || tokenHash.isBlank()) {
            return false;
        }

        try {
            byte[] actualHash = digest(rawToken);
            byte[] expectedHash = HexFormat.of()
                    .parseHex(tokenHash);

            return MessageDigest.isEqual(
                    actualHash,
                    expectedHash
            );
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] digest(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new TokenHashingException("토큰 해시 생성에 실패했습니다.", e);
        }
    }
}