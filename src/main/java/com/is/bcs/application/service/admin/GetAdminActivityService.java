package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.GetAdminActivityUseCase;
import com.is.bcs.application.port.out.admin.GetAdminActivityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAdminActivityService implements GetAdminActivityUseCase {

    private final GetAdminActivityPort getAdminActivityPort;

    @Override
    public Page<Result> getActivities(Pageable pageable, Command command) {
        return getAdminActivityPort.findActivities(pageable, command);
    }
}