package com.is.bcs.iw;

import com.is.bcs.adapter.out.security.jwt.Sha256TokenHasher;
import com.is.bcs.application.port.out.token.TokenHasher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class Sha256TokenHasherTest {

    private final TokenHasher tokenHasher = new Sha256TokenHasher();

    @Test
    void 토큰을_해싱하고_검증한다() {
        String rawToken = "refresh-token";

        String tokenHash = tokenHasher.hash(rawToken);

        assertThat(
                tokenHasher.matches(rawToken, tokenHash)
        ).isTrue();
    }

    @Test
    void 다른_토큰은_일치하지_않는다() {
        String tokenHash = tokenHasher.hash(
                "refresh-token"
        );

        assertThat(
                tokenHasher.matches("other-token", tokenHash)
        ).isFalse();
    }
}