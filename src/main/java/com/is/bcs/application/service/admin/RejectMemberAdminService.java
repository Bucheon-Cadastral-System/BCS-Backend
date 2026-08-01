package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.RejectMemberAdminUseCase;
import com.is.bcs.application.port.out.admin.RecordAdminActivityPort;
import com.is.bcs.application.port.out.admin.RejectMemberAdminPort;
import com.is.bcs.domain.admin.AdminActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RejectMemberAdminService implements RejectMemberAdminUseCase {

    private final RejectMemberAdminPort rejectMemberAdminPort;
    private final RecordAdminActivityPort recordAdminActivityPort;

    @Override
    public void reject(Long actorAdminId, Long targetMemberId)
    {
        rejectMemberAdminPort.reject(targetMemberId);

        recordAdminActivityPort.record(
                actorAdminId,
                targetMemberId,
                AdminActivityType.MEMBER_REJECTED,
                "회원 가입을 거절했습니다."
        );
    }
}