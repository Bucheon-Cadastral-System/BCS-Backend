package com.is.bcs.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth.oauth")
public record OAuthProperties(
        Duration exchangeCodeTtl
) {
}