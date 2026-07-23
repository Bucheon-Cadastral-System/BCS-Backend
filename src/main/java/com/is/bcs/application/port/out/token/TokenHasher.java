package com.is.bcs.application.port.out.token;

public interface TokenHasher {

    String hash(String token);

    boolean matches(String rawToken, String tokenHash);

}