package com.is.bcs.adapter.in.web.common;

import com.is.bcs.adapter.in.security.CurrentMemberIdResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 인증이 있으면 회원 id, 없으면 null — 개발용 개방 구간(permitAll)에서는 미인증 호출이 정상 경로라
 * 값을 요구하는 리졸버를 그대로 쓰면 열려 있는 API 가 전부 막힌다. 인증 강제가 켜지면 여기서 null 이 사라진다.
 */
@Component
@RequiredArgsConstructor
public class OptionalMemberId {

    private final CurrentMemberIdResolver currentMemberIdResolver;

    public Long of(Authentication authentication) {
        // permitAll 통과 요청은 익명 토큰이 실려 온다 — 인증된 주체가 아니므로 작성자 없음으로 본다
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return currentMemberIdResolver.resolve(authentication);
    }
}
