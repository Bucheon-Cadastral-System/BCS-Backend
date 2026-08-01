package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.GetMemberAdminUseCase;
import com.is.bcs.application.port.out.admin.GetMemberAdminPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMemberAdminService implements GetMemberAdminUseCase {

    private final GetMemberAdminPort getMemberAdminPort;

    @Override
    public Page<Result> getMembers(Pageable pageable, Command command) {
        return getMemberAdminPort.findMembers(pageable, command);
    }

}
