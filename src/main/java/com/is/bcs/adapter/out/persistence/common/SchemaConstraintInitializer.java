package com.is.bcs.adapter.out.persistence.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

/**
 * 테이블 간 제약(외래키)을 기동할 때 얹는다.
 *
 * <p>이 제약들은 경합이 앱의 제어를 뚫었을 때 틀린 모양의 데이터가 저장되는 것을 DB 가 최종적으로 막는 층이다.
 * 연관관계 대신 id 컬럼을 쓰는 설계라 엔티티 어노테이션에는 외래키를 선언할 자리가 없고,
 * 스크립트 실행을 설정(spring.sql.init)에 맡기면 application.yml 이 버전 관리에서 빠져 있어
 * 사람마다 스키마가 달라진다 — 그래서 코드에 매어 둔다.
 *
 * <p>스키마 권위가 Flyway 로 넘어가면 이 클래스는 사라지고 스크립트가 그대로 마이그레이션 한 벌이 된다.
 */
@Slf4j
@Component
// 시드 적재보다 먼저 — 넣는 데이터가 제약을 지키는지도 같은 기동에서 함께 드러난다
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class SchemaConstraintInitializer implements CommandLineRunner {

    private static final String SCRIPT = "db/constraints.sql";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        // 스크립트는 매번 다시 실행된다 — 각 제약이 DROP IF EXISTS 로 시작해 몇 번을 걸어도 같은 결과다
        jdbcTemplate.execute((java.sql.Connection connection) -> {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(SCRIPT));
            return null;
        });
        log.info("테이블 간 제약 적용 완료 — {}", SCRIPT);
    }
}
