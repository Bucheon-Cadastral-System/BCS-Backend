package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.PromoteMemberAdminUseCase;
import com.is.bcs.application.port.out.admin.PromoteMemberAdminPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PromoteMemberAdminService implements PromoteMemberAdminUseCase {

    private final PromoteMemberAdminPort promoteMemberAdminPort;

    @Override
    public void promote(Long memberId) {
        promoteMemberAdminPort.promote(memberId);
    }
}