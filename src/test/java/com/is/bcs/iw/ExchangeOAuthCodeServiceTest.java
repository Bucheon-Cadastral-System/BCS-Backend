package com.is.bcs.iw;

import com.is.bcs.application.port.in.auth.ExchangeOAuthCodeUseCase.ExchangeOAuthCodeResult;
import com.is.bcs.application.port.out.token.OAuthCodeStore;
import com.is.bcs.application.service.auth.ExchangeOAuthCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeOAuthCodeServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-07-23T10:00:00Z");

    @Mock
    private OAuthCodeStore oauthCodeStore;

    private ExchangeOAuthCodeService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                NOW,
                ZoneOffset.UTC
        );

        service = new ExchangeOAuthCodeService(
                oauthCodeStore,
                clock
        );
    }

    @Test
    @DisplayName("유효한 일회용 코드를 Access Token으로 교환한다")
    void exchangeSuccess() {
        // given
        String code = "valid-code";
        String accessToken = "access-token";

        OAuthExchangeToken exchangeToken =
                new OAuthExchangeToken(
                        accessToken,
                        NOW.plusSeconds(1800),
                        NOW.plusSeconds(60)
                );

        when(oauthCodeStore.getAndDelete(code))
                .thenReturn(Optional.of(exchangeToken));

        // when
        ExchangeOAuthCodeResult result =
                service.exchange(code);

        // then
        assertThat(result.accessToken())
                .isEqualTo(accessToken);

        assertThat(result.accessTokenExpiresAt())
                .isEqualTo(NOW.plusSeconds(1800));

        verify(oauthCodeStore).getAndDelete(code);
    }

    @Test
    @DisplayName("존재하지 않는 일회용 코드는 교환할 수 없다")
    void exchangeFailsWhenCodeDoesNotExist() {
        // given
        String code = "invalid-code";

        when(oauthCodeStore.getAndDelete(code))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.exchange(code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 일회용 코드입니다.");

        verify(oauthCodeStore).getAndDelete(code);
    }

    @Test
    @DisplayName("만료된 일회용 코드는 교환할 수 없다")
    void exchangeFailsWhenCodeExpired() {
        // given
        String code = "expired-code";

        OAuthExchangeToken exchangeToken =
                new OAuthExchangeToken(
                        "access-token",
                        NOW.plusSeconds(1800),
                        NOW.minusSeconds(1)
                );

        when(oauthCodeStore.getAndDelete(code))
                .thenReturn(Optional.of(exchangeToken));

        // when & then
        assertThatThrownBy(() -> service.exchange(code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("만료된 일회용 코드입니다.");

        verify(oauthCodeStore).getAndDelete(code);
    }

    @Test
    @DisplayName("빈 일회용 코드는 저장소를 조회하지 않는다")
    void exchangeFailsWhenCodeIsBlank() {
        assertThatThrownBy(() -> service.exchange(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("일회용 코드가 비어 있습니다.");

        verifyNoInteractions(oauthCodeStore);
    }
}