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
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
