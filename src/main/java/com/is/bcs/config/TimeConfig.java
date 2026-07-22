package com.is.bcs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 시각의 단일 소스. 시각이 필요한 빈은 이 Clock을 주입받아 OffsetDateTime.now(clock)으로 얻는다
 * — JVM 타임존과 무관하게 KST를 보장하고, 테스트에서 Clock.fixed로 교체해 시간을 고정할 수 있다.
 */
@Configuration
public class TimeConfig {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(KST);
    }
}
