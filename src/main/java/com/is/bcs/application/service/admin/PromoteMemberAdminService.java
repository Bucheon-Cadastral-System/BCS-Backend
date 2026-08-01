package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.PromoteMemberAdminUseCase;
import com.is.bcs.application.port.out.admin.PromoteMemberAdminPort;
import com.is.bcs.application.port.out.admin.RecordAdminActivityPort;
import com.is.bcs.domain.admin.AdminActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PromoteMemberAdminService implements PromoteMemberAdminUseCase {

    private final PromoteMemberAdminPort promoteMemberAdminPort;
    private final RecordAdminActivityPort recordAdminActivityPort;

    @Override
    public void promote(Long actorAdminId, Long targetMemberId) {
        promoteMemberAdminPort.promote(targetMemberId);

        recordAdminActivityPort.record(
                actorAdminId,
                targetMemberId,
                AdminActivityType.MEMBER_PROMOTED_TO_ADMIN,
                "회원의 권한을 USER->ADMIN으로 교체했습니다."
        );
    }
}