package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.ActivateMemberAdminUseCase;
import com.is.bcs.application.port.out.admin.ActivateMemberAdminPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivateMemberAdminService implements ActivateMemberAdminUseCase {

    private final ActivateMemberAdminPort activateMemberAdminPort;

    @Override
    public void activate(Long memberId) {
        activateMemberAdminPort.activate(memberId);
    }
}