package com.is.bcs.application.port.out.admin;

import com.is.bcs.application.port.in.admin.GetAdminActivityUseCase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface GetAdminActivityPort {

    Slice<GetAdminActivityUseCase.Result> findActivities(
            Pageable pageable,
            GetAdminActivityUseCase.Command command
    );
}