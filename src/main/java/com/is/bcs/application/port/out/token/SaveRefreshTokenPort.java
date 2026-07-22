package com.is.bcs.application.port.out.token;

import com.is.bcs.domain.token.RefreshToken;

public interface SaveRefreshTokenPort {

    RefreshToken save(RefreshToken refreshToken);
}
