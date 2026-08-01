package com.is.bcs.application.port.out.admin;

import com.is.bcs.application.port.in.admin.GetAdminActivityUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetAdminActivityPort {

    Page<GetAdminActivityUseCase.Result> findActivities(
            Pageable pageable,
            GetAdminActivityUseCase.Command command
    );
}