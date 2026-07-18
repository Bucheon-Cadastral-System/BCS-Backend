package com.is.bcs.adapter.out.persistence.member;

import com.is.bcs.application.port.out.member.LoadMemberPort;
import com.is.bcs.application.port.out.member.SaveMemberPort;
import com.is.bcs.domain.member.Member;
import com.is.bcs.domain.member.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MemberPersistenceAdapter
        implements LoadMemberPort, SaveMemberPort {

    private final MemberJpaRepository memberJpaRepository;

    @Override
    public Optional<Member> findById(Long memberId) {
        return memberJpaRepository
                .findById(memberId)
                .map(MemberJpaEntity::toDomain);
    }

    @Override
    public Optional<Member> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId) {
        return memberJpaRepository
                .findByProviderAndProviderUserId(
                        provider,
                        providerUserId
                )
                .map(MemberJpaEntity::toDomain);
    }

    @Override
    public Member save(Member member) {
        MemberJpaEntity entity = MemberJpaEntity.fromDomain(member);

        MemberJpaEntity savedEntity = memberJpaRepository.save(entity);

        return savedEntity.toDomain();
    }
}