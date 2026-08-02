package com.is.bcs.application.port.out.admin;

import com.is.bcs.application.port.in.admin.UpdateMemberProfileAdminUseCase;

public interface UpdateMemberProfileAdminPort {

    void updateProfile(Long memberId, UpdateMemberProfileAdminUseCase.Command command);
}