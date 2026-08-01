package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.DeactivateMemberAdminUseCase;
import com.is.bcs.application.port.out.admin.DeactivateMemberAdminPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeactivateMemberAdminService implements DeactivateMemberAdminUseCase {

    private final DeactivateMemberAdminPort deactivateMemberAdminPort;

    @Override
    public void deactivate(Long memberId) {
        deactivateMemberAdminPort.deactivate(memberId);
    }

}