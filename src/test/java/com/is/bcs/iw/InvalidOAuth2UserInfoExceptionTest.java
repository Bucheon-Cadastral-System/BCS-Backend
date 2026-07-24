package com.is.bcs.iw;

import com.is.bcs.adapter.in.security.oauth2.exception.InvalidOAuth2UserInfoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2Error;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InvalidOAuth2UserInfoExceptionTest {

    @Test
    @DisplayName("OAuth2 사용자 정보가 올바르지 않으면 전용 인증 예외가 발생한다")
    void invalidOAuth2UserInfoExceptionTest() {
        // given
        String message = "카카오 사용자 ID를 확인할 수 없습니다.";

        // when
        InvalidOAuth2UserInfoException exception = assertThrows(
                InvalidOAuth2UserInfoException.class,
                () -> {
                    throw new InvalidOAuth2UserInfoException(message);
                }
        );

        // then
        OAuth2Error error = exception.getError();

        assertAll(
                () -> assertEquals(message, exception.getMessage()),
                () -> assertEquals(
                        "invalid_oauth2_user_info",
                        error.getErrorCode()
                )
        );
    }
}