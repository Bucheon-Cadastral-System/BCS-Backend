package com.is.bcs.adapter.out.persistence.admin;

import com.is.bcs.adapter.out.persistence.member.MemberJpaEntity;
import com.is.bcs.adapter.out.persistence.member.MemberJpaRepository;
import com.is.bcs.application.port.out.admin.RejectMemberAdminPort;
import com.is.bcs.domain.member.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
@RequiredArgsConstructor
public class RejectMemberAdminAdapter implements RejectMemberAdminPort {

    private final MemberJpaRepository memberJpaRepository;

    @Override
    public void reject(Long memberId) {
        MemberJpaEntity member = memberJpaRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다. memberId=" + memberId));

        member.reject();
    }
}