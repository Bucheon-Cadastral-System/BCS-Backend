package com.is.bcs.application.port.in.admin;

import com.is.bcs.domain.member.District;
import com.is.bcs.domain.member.Position;
import com.is.bcs.domain.member.Team;

public interface UpdateMemberProfileAdminUseCase {

    void updateProfile(Long memberId, Command command);

    record Command(
            String name,
            String phone,
            String email,
            District district,
            String department,
            Team team,
            Position position
    ) {

        public boolean hasChanges() {
            return name != null
                    || phone != null
                    || email != null
                    || district != null
                    || department != null
                    || team != null
                    || position != null;
        }
    }
}