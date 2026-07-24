package com.is.bcs.adapter.in.security.oauth2;

import com.is.bcs.adapter.in.security.oauth2.exception.InvalidOAuth2UserInfoException;

import java.util.Map;

public class KakaoOAuth2UserInfo {

    private final Map<String, Object> attributes;

    public KakaoOAuth2UserInfo(
            Map<String, Object> attributes
    ) {
        this.attributes = attributes;
    }

    public String providerUserId() {
        Object id = attributes.get("id");

        if (id == null) {
            throw new InvalidOAuth2UserInfoException("카카오 사용자 ID를 확인할 수 없습니다.");
        }

        return String.valueOf(id);
    }
}