package com.is.bcs.adapter.out.persistence.member;

import com.is.bcs.domain.member.OAuthProvider;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long> {

    Optional<MemberJpaEntity> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    /**
     * 상태를 바꾸기 전에 그 회원 행을 잠근다.
     *
     * <p>전이는 현재 상태를 읽어 검사하고 새 상태를 쓰는 일이고, 한 요청 안에서 끝난다.
     * 잠그지 않으면 두 관리자가 같은 회원을 동시에 승인하고 거절할 때 둘 다 검사를 통과해
     * 나중에 커밋한 쪽이 조용히 덮는다. 잠그면 두 번째가 바뀐 상태를 보고 제 규칙대로 거절한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MemberJpaEntity m where m.id = :id")
    Optional<MemberJpaEntity> findByIdForUpdate(@Param("id") Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

}
