package com.is.bcs.application.port.in.admin;

public interface ActivateMemberAdminUseCase {

    void activate(Long actorAdminId, Long memberId);
}