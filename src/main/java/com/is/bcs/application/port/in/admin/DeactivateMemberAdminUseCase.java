package com.is.bcs.application.port.in.admin;

public interface DeactivateMemberAdminUseCase {

    void deactivate(Long actorAdminId, Long memberId);
}