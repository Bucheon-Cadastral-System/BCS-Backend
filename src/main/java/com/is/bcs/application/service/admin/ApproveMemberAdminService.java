package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.ApproveMemberAdminUseCase;
import com.is.bcs.application.port.out.admin.ApproveMemberAdminPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ApproveMemberAdminService implements ApproveMemberAdminUseCase {

    private final ApproveMemberAdminPort approveMemberAdminPort;

    @Override
    public void approve(Long memberId) {
        approveMemberAdminPort.approve(memberId);
    }

}