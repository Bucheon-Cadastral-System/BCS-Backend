package com.is.bcs.application.port.in.admin;

public interface DemoteMemberAdminUseCase {

    void demote(Long actorAdminId, Long memberId);
}