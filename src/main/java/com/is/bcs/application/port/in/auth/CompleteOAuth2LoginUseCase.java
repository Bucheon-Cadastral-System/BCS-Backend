package com.is.bcs.application.port.in.auth;

public interface CompleteOAuth2LoginUseCase {

    Result complete(Command command);

    record Command(
            Long memberId,
            String codeChallenge
    ) {
    }

    record Result(
            String exchangeCode
    ) {
    }
}