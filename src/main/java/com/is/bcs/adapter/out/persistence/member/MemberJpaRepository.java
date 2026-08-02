package com.is.bcs.adapter.out.persistence.member;

import com.is.bcs.domain.member.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long> {

    Optional<MemberJpaEntity> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    boolean existsByEmailAndIdNot(String email, Long id);

}
