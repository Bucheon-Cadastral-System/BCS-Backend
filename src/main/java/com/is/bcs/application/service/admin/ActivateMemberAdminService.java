package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.ActivateMemberAdminUseCase;
import com.is.bcs.application.port.out.admin.ActivateMemberAdminPort;
import com.is.bcs.application.port.out.admin.RecordAdminActivityPort;
import com.is.bcs.domain.admin.AdminActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivateMemberAdminService implements ActivateMemberAdminUseCase {

    private final ActivateMemberAdminPort activateMemberAdminPort;
    private final RecordAdminActivityPort recordAdminActivityPort;

    @Override
    public void activate(Long actorAdminId, Long targetMemberId) {
        activateMemberAdminPort.activate(targetMemberId);

        recordAdminActivityPort.record(
                actorAdminId,
                targetMemberId,
                AdminActivityType.MEMBER_ACTIVATED,
                "회원을 활성화했습니다."
        );
    }
}