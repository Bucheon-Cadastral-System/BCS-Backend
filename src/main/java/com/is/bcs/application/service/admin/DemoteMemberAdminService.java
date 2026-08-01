package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.DemoteMemberAdminUseCase;
import com.is.bcs.application.port.out.admin.DemoteMemberAdminPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DemoteMemberAdminService implements DemoteMemberAdminUseCase {

    private final DemoteMemberAdminPort demoteMemberAdminPort;

    @Override
    public void demote(Long memberId) {
        demoteMemberAdminPort.demote(memberId);
    }
}