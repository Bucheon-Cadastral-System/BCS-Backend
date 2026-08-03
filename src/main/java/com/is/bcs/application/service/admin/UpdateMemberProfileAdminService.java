package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.UpdateMemberProfileAdminUseCase;
import com.is.bcs.application.port.out.admin.RecordAdminActivityPort;
import com.is.bcs.application.port.out.admin.UpdateMemberProfileAdminPort;
import com.is.bcs.application.port.out.member.LoadMemberPort;
import com.is.bcs.domain.admin.AdminActivityType;
import com.is.bcs.domain.member.Member;
import com.is.bcs.domain.member.exception.InvalidMemberProfileException;
import com.is.bcs.domain.member.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateMemberProfileAdminService implements UpdateMemberProfileAdminUseCase {

    private final UpdateMemberProfileAdminPort updateMemberProfileAdminPort;
    private final RecordAdminActivityPort recordAdminActivityPort;
    private final LoadMemberPort loadMemberPort;

    @Override
    public void updateProfile(Long actorAdminId, Long targetMemberId, Command command) {
        if (!command.hasChanges()) {
            throw new InvalidMemberProfileException("변경할 회원 정보를 한 개 이상 입력해야 합니다.");
        }

        updateMemberProfileAdminPort.updateProfile(targetMemberId, command);

        Member actorAdmin = loadMemberPort.findById(actorAdminId)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다. memberId=" + actorAdminId));

        Member targetMember = loadMemberPort.findById(targetMemberId)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다. memberId=" + actorAdminId));

        recordAdminActivityPort.record(
                actorAdminId,
                targetMemberId,
                AdminActivityType.MEMBER_PROFILE_UPDATED,
                "%s님(ID: %d)이 %s 회원(ID: %d)을 회원의 정보를 업데이트했습니다."
                        .formatted(
                                actorAdmin.getName(),
                                actorAdminId,
                                targetMember.getName(),
                                targetMemberId
                        )
        );
    }
}