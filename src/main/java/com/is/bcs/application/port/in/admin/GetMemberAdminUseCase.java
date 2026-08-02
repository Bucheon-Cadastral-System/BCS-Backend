package com.is.bcs.application.port.in.admin;

import com.is.bcs.domain.member.District;
import com.is.bcs.domain.member.MemberRole;
import com.is.bcs.domain.member.MemberStatus;
import com.is.bcs.domain.member.Position;
import com.is.bcs.domain.member.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetMemberAdminUseCase {

    Page<Result> getMembers(Pageable pageable, Command command);

    record Command(
            String name,
            String email,
            String phone,
            District district,
            Team team,
            Position position,
            MemberStatus memberStatus,
            MemberRole memberRole
    ) {
    }

    record Result(
            Long id,
            String name,
            String email,
            String phone,
            District district,
            Team team,
            Position position,
            MemberStatus memberStatus,
            MemberRole memberRole
    ) {
    }
}