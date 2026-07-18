package com.is.bcs.application.port.out.member;

import com.is.bcs.domain.member.Member;

public interface SaveMemberPort {

    Member save(Member member);

}