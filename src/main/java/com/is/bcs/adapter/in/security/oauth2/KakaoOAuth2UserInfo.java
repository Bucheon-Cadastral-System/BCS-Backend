package com.is.bcs.adapter.in.security.oauth2;

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
            throw new IllegalArgumentException("카카오 사용자 ID가 없습니다.");
        }

        return String.valueOf(id);
    }
}