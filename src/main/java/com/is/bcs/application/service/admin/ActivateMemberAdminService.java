package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.ActivateMemberAdminUseCase;
import com.is.bcs.application.port.out.admin.ActivateMemberAdminPort;
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
public class ActivateMemberAdminService implements ActivateMemberAdminUseCase {

    private final ActivateMemberAdminPort activateMemberAdminPort;
    private final RecordAdminActivityPort recordAdminActivityPort;
    private final LoadMemberPort loadMemberPort;

    @Override
    public void activate(Long actorAdminId, Long targetMemberId) {
        activateMemberAdminPort.activate(targetMemberId);

        Member actorAdmin = loadMemberPort.findById(actorAdminId)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다. memberId=" + actorAdminId));

        Member targetMember = loadMemberPort.findById(targetMemberId)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다. memberId=" + targetMemberId));

        recordAdminActivityPort.record(
                actorAdminId,
                targetMemberId,
                AdminActivityType.MEMBER_ACTIVATED,
                "%s님(ID: %d)이 %s 회원(ID: %d)을 활성화했습니다."
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