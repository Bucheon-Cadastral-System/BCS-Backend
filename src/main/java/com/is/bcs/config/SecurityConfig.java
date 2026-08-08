package com.is.bcs.config;

import com.is.bcs.adapter.in.security.jwt.AccessTokenAuthenticationFilter;
import com.is.bcs.adapter.in.security.oauth2.CustomOAuth2UserService;
import com.is.bcs.adapter.in.security.oauth2.OAuth2LoginFailureHandler;
import com.is.bcs.adapter.in.security.oauth2.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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

    /**
     * 관리 엔드포인트(/actuator) 전용 체인 — 아래 본 체인보다 먼저 걸린다.
     *
     * <p>액추에이터는 management.server.port 로 갈라 둔 별도 포트에서만 뜬다. 그 포트는 도커 compose 에서
     * publish 하지 않으므로 바깥에서는 닿지 않고, 같은 네트워크에 있는 수집기(Prometheus)만 긁는다.
     * 여기서 인증을 걸지 않는 이유가 그것이다 — 스크레이퍼는 우리 JWT 를 붙일 수단이 없다.
     * 반대로 8080 으로 들어온 /actuator 요청은 이 matcher 가 잡지 않고(다른 포트다) 본 체인으로 넘어간다.
     *
     * <p>본 체인을 나중에 닫을 때 이 체인을 함께 손볼 필요는 없다. 오히려 이 체인이 없으면
     * 본 체인의 anyRequest().authenticated() 가 관리 포트까지 덮어 수집이 401 로 끊긴다.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain managementSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // 수집기는 GET 만 보내고 세션도 쿠키도 쓰지 않는다
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
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

                .authorizeHttpRequests(auth -> auth
                        /** 관리자(ADMIN) 만 가능한 곳 */
//                        .requestMatchers(
//                                "/api/admin/**"
//                        ).hasRole("ADMIN")

                        /** 개발 단계: 모든 요청 허용. 운영 전환 시 공개/회원/관리자 경로를 분리한다. */
                        .requestMatchers(
//                                "/",
//                                "/error",
//                                "/login/**",
//                                "/oauth2/**",
//
//                                "/swagger-ui.html",
//                                "/swagger-ui/**",
//
//                                "/v3/api-docs",
//                                "/v3/api-docs/**",
//                                "/item/v3/api-docs",
//                                "/item/v3/api-docs/**",
//
//                                "/api/auth/oauth2/kakao",
//                                "/api/auth/token/exchange",
//                                "/api/auth/token/refresh",
//                                "/api/auth/logout",
//                                "/api/csrf",
                                "/**"

                        ).permitAll()

                        // 그 외
                        .anyRequest()
                        .authenticated()
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


    /**
     * CORS 설정
     */
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


}
