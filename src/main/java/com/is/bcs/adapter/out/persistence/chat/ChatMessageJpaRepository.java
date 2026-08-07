package com.is.bcs.adapter.out.persistence.chat;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageJpaEntity, Long> {

    /** 최근 줄부터 limit 만큼 — 화면이 오래된 순으로 그리므로 어댑터가 뒤집는다. */
    List<ChatMessageJpaEntity> findByMemberIdOrderByIdDesc(Long memberId, Limit limit);

    void deleteByMemberId(Long memberId);

    /**
     * 최근 keep 줄만 남기고 그 계정의 나머지를 지운다.
     * 셋을 먼저 세어 보고 지우면 그 사이에 새 줄이 들어와 기준이 어긋나므로 한 문장으로 둔다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            delete from bcs.chat_messages
            where member_id = :memberId
              and id not in (
                select id from bcs.chat_messages
                where member_id = :memberId
                order by id desc
                limit :keep)
            """, nativeQuery = true)
    void deleteOlderThanRecent(@Param("memberId") Long memberId, @Param("keep") int keep);
}
