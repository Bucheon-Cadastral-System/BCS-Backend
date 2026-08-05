package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.ApproveMemberAdminUseCase;
import com.is.bcs.application.port.out.admin.ApproveMemberAdminPort;
import com.is.bcs.application.port.out.admin.RecordAdminActivityPort;
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
public class ApproveMemberAdminService implements ApproveMemberAdminUseCase {

    private final ApproveMemberAdminPort approveMemberAdminPort;
    private final RecordAdminActivityPort recordAdminActivityPort;
    private final LoadMemberPort loadMemberPort;


    @Override
    public void approve(Long actorAdminId, Long targetMemberId) {
        approveMemberAdminPort.approve(targetMemberId);

        Member actorAdmin = loadMemberPort.findById(actorAdminId)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다. memberId=" + actorAdminId));

        Member targetMember = loadMemberPort.findById(targetMemberId)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다. memberId=" + targetMemberId));

        recordAdminActivityPort.record(
                actorAdminId,
                targetMemberId,
                AdminActivityType.MEMBER_APPROVED,
                "%s님(ID: %d)이 %s 회원(ID: %d)을 회원가입 승인했습니다."
                        .formatted(
                                actorAdmin.getName(),
                                actorAdminId,
                                targetMember.getName(),
                                targetMemberId
                        ),
                actorAdmin.getName(),
                targetMember.getName()
        );
    }

}