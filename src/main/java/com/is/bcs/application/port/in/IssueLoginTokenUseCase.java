package com.is.bcs.application.port.in;

import com.is.bcs.application.dto.LoginTokenResult;
import com.is.bcs.domain.member.MemberRole;

public interface IssueLoginTokenUseCase {

    LoginTokenResult issue(Long memberId, MemberRole role);
}