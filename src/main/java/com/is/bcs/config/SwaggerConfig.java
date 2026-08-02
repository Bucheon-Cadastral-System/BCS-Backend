package com.is.bcs.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    public static final String CSRF_SECURITY_SCHEME = "CSRF Token";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("BCS API")
                        .description("부천시 지적기준점 관리 시스템 API 문서")
                        .version("v1.0"))

                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("[Default] local 8080 포트"))

                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))

                        .addSecuritySchemes(CSRF_SECURITY_SCHEME,
                                new SecurityScheme()
                                        .name("X-XSRF-TOKEN")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("GET /api/csrf에서 발급받은 CSRF 토큰")
                        )
                )

                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));

    }
}