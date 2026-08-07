package com.is.bcs.adapter.out.persistence.admin;

import com.is.bcs.adapter.out.persistence.member.MemberJpaEntity;
import com.is.bcs.adapter.out.persistence.member.MemberJpaRepository;
import com.is.bcs.application.port.out.admin.PromoteMemberAdminPort;
import com.is.bcs.domain.member.Member;
import com.is.bcs.domain.member.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PromoteMemberAdminAdapter implements PromoteMemberAdminPort {

    private final MemberJpaRepository memberJpaRepository;

    @Override
    public void promote(Long memberId) {
        MemberJpaEntity entity = memberJpaRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다. memberId=" + memberId));

        Member member = entity.toDomain();
        member.promoteToAdmin();

        memberJpaRepository.save(MemberJpaEntity.fromDomain(member));
    }
}
