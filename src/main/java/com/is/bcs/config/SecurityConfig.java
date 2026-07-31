package com.is.bcs.config;

import com.is.bcs.adapter.in.security.jwt.AccessTokenAuthenticationFilter;
import com.is.bcs.adapter.in.security.oauth2.CustomOAuth2UserService;
import com.is.bcs.adapter.in.security.oauth2.OAuth2LoginFailureHandler;
import com.is.bcs.adapter.in.security.oauth2.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http

                .cors(Customizer.withDefaults())

                /*
                 * CSRF 미사용.
                 * 이 API 는 세션 쿠키가 아니라 Authorization 헤더의 액세스 토큰으로 인증하고,
                 * 브라우저는 그 헤더를 자동으로 붙이지 않으므로 교차 사이트 요청이 남의 권한으로 실행되지 않는다.
                 * 자동 전송되는 값은 리프레시 쿠키뿐인데 SameSite=Lax 라 교차 사이트 POST 에는 실리지 않는다.
                 * 켜 두면 SPA 의 모든 POST·PUT·DELETE 가 토큰 없이 403 으로 막힌다.
                 */
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        /** 모두 사용 가능 ! */
                        .requestMatchers(
                                "/",
                                "/error",
                                "/login/**",
                                "/oauth2/**",

                                "/swagger-ui.html",
                                "/swagger-ui/**",

                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/item/v3/api-docs",
                                "/item/v3/api-docs/**",

                                "/api/auth/token/exchange",
                                "/api/auth/token/refresh",
                                "/api/auth/logout",

                                /*
                                 * 개발 중 전체 개방.
                                 * 앞선 매처가 먼저 맞으므로 이 한 줄이 아래 ADMIN 규칙과 인증 요구를 모두 무력화한다.
                                 * 지우기 전에 /error 를 열어 두어야 예외 응답이 401 로 바뀌지 않는다.
                                 */
                                "/**"

                        ).permitAll()


                        /** 관리자(ADMIN) 만 가능한 곳! */
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

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