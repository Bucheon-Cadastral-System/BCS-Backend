package com.is.bcs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * JPA Auditing 설정 — 생성/수정 시각(@CreatedDate·@LastModifiedDate) 자동 기록.
 * DateTimeProvider를 연결하지 않으면 auditing이 JVM 기본 타임존으로 시각을 만들어
 * Clock(KST) 정책을 우회하므로 반드시 함께 등록한다.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaConfig {

    /** Auditing이 찍는 시각도 Clock(KST)을 따르게 한다. */
    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(OffsetDateTime.now(clock));
    }
}
