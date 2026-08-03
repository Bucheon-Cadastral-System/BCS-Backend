package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.RejectMemberAdminUseCase;
import com.is.bcs.application.port.out.admin.RecordAdminActivityPort;
import com.is.bcs.application.port.out.admin.RejectMemberAdminPort;
import com.is.bcs.application.port.out.member.LoadMemberPort;
import com.is.bcs.domain.admin.AdminActivityType;
import com.is.bcs.domain.member.Member;
import com.is.bcs.domain.member.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RejectMemberAdminService implements RejectMemberAdminUseCase {

    private final RejectMemberAdminPort rejectMemberAdminPort;
    private final RecordAdminActivityPort recordAdminActivityPort;
    private final LoadMemberPort loadMemberPort;

    @Override
    public void reject(Long actorAdminId, Long targetMemberId)
    {
        rejectMemberAdminPort.reject(targetMemberId);

        Member actorAdmin = loadMemberPort.findById(actorAdminId)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다. memberId=" + actorAdminId));

        Member targetMember = loadMemberPort.findById(targetMemberId)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다. memberId=" + actorAdminId));

        recordAdminActivityPort.record(
                actorAdminId,
                targetMemberId,
                AdminActivityType.MEMBER_REJECTED,
                "%s님(ID: %d)이 %s 회원(ID: %d)을 회원가입 거절했습니다."
                        .formatted(
                                actorAdmin.getName(),
                                actorAdminId,
                                targetMember.getName(),
                                targetMemberId
                        )
        );
    }
}