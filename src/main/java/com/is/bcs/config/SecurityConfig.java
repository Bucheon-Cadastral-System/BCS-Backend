package com.is.bcs.config;

import com.is.bcs.adapter.in.security.jwt.AccessTokenAuthenticationFilter;
import com.is.bcs.adapter.in.security.oauth2.CustomOAuth2UserService;
import com.is.bcs.adapter.in.security.oauth2.OAuth2LoginFailureHandler;
import com.is.bcs.adapter.in.security.oauth2.OAuth2SuccessHandler;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final AccessTokenAuthenticationFilter accessTokenAuthenticationFilter;
    private final OAuth2LoginFailureHandler oauth2LoginFailureHandler;

    /** 8080 으로 들어오는 일반적인 시큐리티 필터체인 */
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http

                .cors(Customizer.withDefaults())
                /** CSRF 설정 (각 세션마다 CSRF 토큰 만들어서 세션에 저장) */
                .csrf(csrf -> csrf
                        .csrfTokenRepository(
                                CookieCsrfTokenRepository.withHttpOnlyFalse()
                        )
                        .requireCsrfProtectionMatcher(
                                new RegexRequestMatcher("^/api/members/me/registration$", "PUT")
                        )
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN))
                )

                .authorizeHttpRequests(auth -> auth
                        // /error 재디스패치를 다시 차단하지 않는다.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        // 인증 및 가입 과정
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/**",
                                "/api/auth/oauth2/kakao",
                                "/api/auth/token/exchange",
                                "/api/auth/token/refresh",
                                "/api/auth/logout",
                                "/api/csrf"
                        ).permitAll()

                        // 게스트에게 공개하는 기준점 기본 정보
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/control-points/public",
                                "/api/control-points/public/**"
                        ).permitAll()

                        // 가입 대기 회원도 필요한 API
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/members/me/registration"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/members/me/state"
                        ).authenticated()

                        // 관리자 API
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        // 모든 회원의 기준점 등록·수정·삭제
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/control-points"
                        ).hasAnyRole("USER", "ADMIN")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/control-points/**"
                        ).hasAnyRole("USER", "ADMIN")
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/control-points/**"
                        ).hasAnyRole("USER", "ADMIN")

                        // 모든 회원의 프로젝트 및 프로젝트 하위 자원
                        .requestMatchers(
                                "/api/survey-projects/**"
                        ).hasAnyRole("USER", "ADMIN")

                        // 모든 회원의 사진 파일 접근
                        .requestMatchers(
                                "/api/control-point-images/**"
                        ).hasAnyRole("USER", "ADMIN")

                        // 기준점 마스터 파일 처리
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/imports/control-points",
                                "/api/imports/control-points/preview"
                        ).hasAnyRole("USER", "ADMIN")

                        // 조사 파일 처리
                        .requestMatchers(
                                "/api/imports/**"
                        ).hasAnyRole("USER", "ADMIN")

                        // 내부 기준점 조회
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/control-points",
                                "/api/control-points/**"
                        ).hasAnyRole("USER", "ADMIN")

                        // 회원 본인 API
                        .requestMatchers(
                                "/api/members/**"
                        ).hasAnyRole("USER", "ADMIN")

                        // 챗봇
                        .requestMatchers(
                                "/api/chat/**",
                                "/api/chat"
                        ).hasAnyRole("USER", "ADMIN")

                        // Swagger.
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/item/v3/api-docs",
                                "/item/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers("/", "/error").permitAll()

                        // 정책에 명시되지 않은 신규 API는 자동 차단
                        .requestMatchers("/api/**").denyAll()
                        .anyRequest().denyAll()
                )

                /** Kakao Oauth2.0 Filter */
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oauth2LoginFailureHandler)
                )

                /** AccessToken 검증 Filter */
                .addFilterBefore(
                        accessTokenAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    /** CORS 설정 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://bcs.inwoohub.com"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /** 8081 으로 들어오는 Actuator 시큐리티 필터체인 */
    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/actuator/**")    // 해당 경로로 들어오는 것만 잡아서 처리
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/prometheus"
                        ).permitAll()
                        .anyRequest().permitAll() // 모든 요청 수락 가능
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .build();
    }

}
