package com.is.bcs.application.port.in.auth;

public interface LogoutUseCase {

    void logout(String refreshToken);
}