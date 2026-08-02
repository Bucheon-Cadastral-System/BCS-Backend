package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.ApproveMemberAdminUseCase;
import com.is.bcs.application.port.out.admin.ApproveMemberAdminPort;
import com.is.bcs.application.port.out.admin.RecordAdminActivityPort;
import com.is.bcs.domain.admin.AdminActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ApproveMemberAdminService implements ApproveMemberAdminUseCase {

    private final ApproveMemberAdminPort approveMemberAdminPort;
    private final RecordAdminActivityPort recordAdminActivityPort;


    @Override
    public void approve(Long actorAdminId, Long targetMemberId) {
        approveMemberAdminPort.approve(targetMemberId);

        recordAdminActivityPort.record(
                actorAdminId,
                targetMemberId,
                AdminActivityType.MEMBER_APPROVED,
                "회원 가입을 승인했습니다."
        );
    }

}