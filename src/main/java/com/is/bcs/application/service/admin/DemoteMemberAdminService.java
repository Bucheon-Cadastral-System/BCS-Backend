package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.DemoteMemberAdminUseCase;
import com.is.bcs.application.port.out.admin.DemoteMemberAdminPort;
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
public class DemoteMemberAdminService implements DemoteMemberAdminUseCase {

    private final DemoteMemberAdminPort demoteMemberAdminPort;
    private final RecordAdminActivityPort recordAdminActivityPort;
    private final LoadMemberPort loadMemberPort;

    @Override
    public void demote(Long actorAdminId, Long targetMemberId) {
        demoteMemberAdminPort.demote(targetMemberId);

        Member actorAdmin = loadMemberPort.findById(actorAdminId)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다. memberId=" + actorAdminId));

        Member targetMember = loadMemberPort.findById(targetMemberId)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다. memberId=" + actorAdminId));

        recordAdminActivityPort.record(
                actorAdminId,
                targetMemberId,
                AdminActivityType.MEMBER_DEMOTED_TO_USER,
                "%s님(ID: %d)이 %s 회원(ID: %d)을 USER로 권한을 변경했습니다."
                        .formatted(
                                actorAdmin.getName(),
                                actorAdminId,
                                targetMember.getName(),
                                targetMemberId
                        )
        );
    }
}