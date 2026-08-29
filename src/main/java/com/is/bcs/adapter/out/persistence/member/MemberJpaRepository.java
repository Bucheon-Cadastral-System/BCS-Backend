package com.is.bcs.adapter.out.persistence.member;

import com.is.bcs.domain.member.MemberStatus;
import com.is.bcs.domain.member.OAuthProvider;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long> {

    Optional<MemberJpaEntity> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    /**
     * 상태를 바꾸기 전에 그 회원 행을 잠근다. Select ~ for update (비관적 락)
     *
     * <p>전이는 현재 상태를 읽어 검사하고 새 상태를 쓰는 일이고, 한 요청 안에서 끝난다.
     * 잠그지 않으면 두 관리자가 같은 회원을 동시에 승인하고 거절할 때 둘 다 검사를 통과해
     * 나중에 커밋한 쪽이 조용히 덮는다. 잠그면 두 번째가 바뀐 상태를 보고 제 규칙대로 거절한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MemberJpaEntity m where m.id = :id")
    Optional<MemberJpaEntity> findByIdForUpdate(@Param("id") Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    // SELECT *
    // FROM members
    // WHERE status = ?
    // AND requested_at <= ?;
    List<MemberJpaEntity> findAllByStatusAndRequestedAtLessThanEqual(MemberStatus status, OffsetDateTime cutoff);

    /**
     * Modifying : Spring Data JPA 해당 쿼리는 데이터 조회가 아닌, 변경을 위한 쿼리임 (Delete 사용)
     * flushAutomatically : 삭제 쿼리 실행 전 영속성 컨텍스트 쌓인 변경 사항을 먼저 DB에 반영 (flush -> DB 반영 -> DELETE)
     * clearAutomatically : 삭제 쿼리 실행 후 영속석 컨텍스트 비우기
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from MemberJpaEntity m
            where m.id = :memberId
              and m.status = :status
              and m.requestedAt <= :cutoff
              and (
                    m.name is null
                    or trim(m.name) = ''
                    or m.phone is null
                    or trim(m.phone) = ''
                    or m.email is null
                    or trim(m.email) = ''
                    or m.district is null
                    or m.department is null
                    or trim(m.department) = ''
                    or m.team is null
                    or m.position is null
              )
            """)
    int deleteIfExpiredAndIncomplete(
            @Param("memberId") Long memberId,
            @Param("status") MemberStatus status,
            @Param("cutoff") OffsetDateTime cutoff
    );

}
