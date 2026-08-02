package com.is.bcs.application.port.out.admin;

import com.is.bcs.application.port.in.admin.GetMemberAdminUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetMemberAdminPort {

    Page<GetMemberAdminUseCase.Result> findMembers(Pageable pageable, GetMemberAdminUseCase.Command command);

}