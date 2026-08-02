package com.is.bcs.adapter.in.security;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CsrfController {

    @Operation(summary = "CSRF 토큰 발급",
            description = "세션 기반 회원가입 정보 입력(PUT /api/members/me/registration)에 필요한 CSRF 토큰을 발급합니다.")
    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }

}