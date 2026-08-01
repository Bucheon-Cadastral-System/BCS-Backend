package com.is.bcs.application.port.in.admin;

public interface ApproveMemberAdminUseCase {

    void approve(Long actorAdminId, Long targetMemberId);

}