package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.RejectMemberAdminUseCase;
import com.is.bcs.application.port.out.admin.RejectMemberAdminPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RejectMemberAdminService
        implements RejectMemberAdminUseCase {

    private final RejectMemberAdminPort rejectMemberAdminPort;

    @Override
    public void reject(Long memberId) {
        rejectMemberAdminPort.reject(memberId);
    }
}