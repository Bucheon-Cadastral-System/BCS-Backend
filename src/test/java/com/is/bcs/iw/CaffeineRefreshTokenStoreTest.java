package com.is.bcs.iw;

import com.is.bcs.application.port.out.token.RefreshTokenStore;
import com.is.bcs.domain.token.RefreshToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class CaffeineRefreshTokenStoreTest {

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    @Test
    void refreshToken을_저장하고_조회한다() {
        RefreshToken token = new RefreshToken(
                "token-id",
                1L,
                "hashed-token",
                Instant.now().plusSeconds(3600)
        );

        refreshTokenStore.save(token);

        RefreshToken found = refreshTokenStore
                .findByTokenId("token-id")
                .orElseThrow();

        assertThat(found.memberId()).isEqualTo(1L);
        assertThat(found.tokenHash()).isEqualTo("hashed-token");
    }

    @Test
    void refreshToken을_삭제한다() {
        RefreshToken token = new RefreshToken(
                "token-id",
                1L,
                "hashed-token",
                Instant.now().plusSeconds(3600)
        );

        refreshTokenStore.save(token);
        refreshTokenStore.deleteByTokenId("token-id");

        assertThat(
                refreshTokenStore.findByTokenId("token-id")
        ).isEmpty();
    }
}