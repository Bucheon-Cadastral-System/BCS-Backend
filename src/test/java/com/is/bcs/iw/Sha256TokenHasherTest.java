package com.is.bcs.iw;

import com.is.bcs.adapter.out.security.jwt.Sha256TokenHasher;
import com.is.bcs.domain.token.exception.TokenHashingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Sha256TokenHasherTest {

    private final Sha256TokenHasher tokenHasher = new Sha256TokenHasher();

    @Test
    @DisplayName("토큰을 SHA-256 해시로 변환한다")
    void hashesToken() {
        String hash = tokenHasher.hash("refresh-token");

        assertEquals(64, hash.length());
    }

    @Test
    @DisplayName("원본 토큰과 저장된 해시가 일치하면 true를 반환한다")
    void matchesToken() {
        String rawToken = "refresh-token";
        String tokenHash = tokenHasher.hash(rawToken);

        assertTrue(
                tokenHasher.matches(rawToken, tokenHash)
        );
    }

    @Test
    @DisplayName("원본 토큰과 저장된 해시가 다르면 false를 반환한다")
    void doesNotMatchDifferentToken() {
        String tokenHash =
                tokenHasher.hash("refresh-token");

        assertFalse(
                tokenHasher.matches(
                        "different-token",
                        tokenHash
                )
        );
    }

    @Test
    @DisplayName("해싱할 토큰이 비어 있으면 예외가 발생한다")
    void rejectsBlankToken() {
        TokenHashingException exception = assertThrows(
                TokenHashingException.class,
                () -> tokenHasher.hash(" ")
        );

        assertEquals(
                "해싱할 토큰이 비어 있습니다.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("저장된 해시 형식이 올바르지 않으면 false를 반환한다")
    void returnsFalseForMalformedHash() {
        assertFalse(
                tokenHasher.matches(
                        "refresh-token",
                        "invalid-hash"
                )
        );
    }
}