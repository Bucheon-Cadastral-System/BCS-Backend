package com.is.bcs.application.port.out.member;

import com.is.bcs.domain.member.Member;
import com.is.bcs.domain.member.OAuthProvider;

import java.util.Optional;

public interface LoadMemberPort {

    Optional<Member> findById(Long memberId);

    Optional<Member> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

}