package com.is.bcs.adapter.in.security.oauth2;

import com.is.bcs.application.dto.OAuthLoginCommand;
import com.is.bcs.application.dto.OAuthLoginResult;
import com.is.bcs.application.port.in.oauth.OAuthLoginUseCase;
import com.is.bcs.domain.member.OAuthProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuthLoginUseCase oauthLoginUseCase;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oauth2User = delegate.loadUser(userRequest); // 여기서 Kakao로 유저 정보 Get

        String registrationId = userRequest
                .getClientRegistration()
                .getRegistrationId();

        if (!"kakao".equals(registrationId)) {
            throw new OAuth2AuthenticationException("지원하지 않는 OAuth Provider: " + registrationId);
        }

        KakaoOAuth2UserInfo kakao = new KakaoOAuth2UserInfo(oauth2User.getAttributes());

        OAuthLoginResult result = oauthLoginUseCase.login(new OAuthLoginCommand(OAuthProvider.KAKAO, kakao.providerUserId()));

        log.info("로그인 사용자 : {}, Role : {}", result.memberId(), result.role());

        return new BcsOAuth2Principal(
                result.memberId(),
                result.role(),
                result.status(),
                result.profileCompleted(),
                oauth2User.getAttributes()
        );

    }
}