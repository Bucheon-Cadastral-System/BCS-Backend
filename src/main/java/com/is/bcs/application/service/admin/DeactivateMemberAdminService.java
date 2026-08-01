package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.DeactivateMemberAdminUseCase;
import com.is.bcs.application.port.out.admin.DeactivateMemberAdminPort;
import com.is.bcs.application.port.out.admin.RecordAdminActivityPort;
import com.is.bcs.domain.admin.AdminActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeactivateMemberAdminService implements DeactivateMemberAdminUseCase {

    private final DeactivateMemberAdminPort deactivateMemberAdminPort;
    private final RecordAdminActivityPort recordAdminActivityPort;

    @Override
    public void deactivate(Long actorAdminId, Long targetMemberId) {
        deactivateMemberAdminPort.deactivate(targetMemberId);

        recordAdminActivityPort.record(
                actorAdminId,
                targetMemberId,
                AdminActivityType.MEMBER_DEACTIVATED,
                "회원을 비활성화했습니다."
        );
    }

}