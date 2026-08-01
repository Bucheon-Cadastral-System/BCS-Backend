package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.DemoteMemberAdminUseCase;
import com.is.bcs.application.port.out.admin.DemoteMemberAdminPort;
import com.is.bcs.application.port.out.admin.RecordAdminActivityPort;
import com.is.bcs.domain.admin.AdminActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DemoteMemberAdminService implements DemoteMemberAdminUseCase {

    private final DemoteMemberAdminPort demoteMemberAdminPort;
    private final RecordAdminActivityPort recordAdminActivityPort;

    @Override
    public void demote(Long actorAdminId, Long targetMemberId) {
        demoteMemberAdminPort.demote(targetMemberId);

        recordAdminActivityPort.record(
                actorAdminId,
                targetMemberId,
                AdminActivityType.MEMBER_DEMOTED_TO_USER,
                "회원의 권한을 ADMIN->USER으로 교체했습니다."
        );
    }
}