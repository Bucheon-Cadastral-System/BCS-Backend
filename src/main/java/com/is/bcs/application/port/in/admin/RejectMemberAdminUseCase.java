package com.is.bcs.application.port.in.admin;

public interface RejectMemberAdminUseCase {

    void reject(Long actorAdminId,Long memberId);

}