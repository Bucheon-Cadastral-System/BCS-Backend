package com.is.bcs.adapter.out.persistence.admin;

import com.is.bcs.adapter.out.persistence.member.MemberJpaEntity;
import com.is.bcs.adapter.out.persistence.member.MemberJpaRepository;
import com.is.bcs.application.port.out.admin.DemoteMemberAdminPort;
import com.is.bcs.domain.member.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DemoteMemberAdminAdapter implements DemoteMemberAdminPort {

    private final MemberJpaRepository memberJpaRepository;

    @Override
    public void demote(Long memberId) {
        MemberJpaEntity member = memberJpaRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다. memberId=" + memberId));

        member.demoteToUser();
    }
}