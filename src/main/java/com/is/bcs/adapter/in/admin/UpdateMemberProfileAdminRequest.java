package com.is.bcs.adapter.in.admin;

import com.is.bcs.application.port.in.admin.UpdateMemberProfileAdminUseCase;
import com.is.bcs.domain.member.District;
import com.is.bcs.domain.member.Position;
import com.is.bcs.domain.member.Team;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMemberProfileAdminRequest(
        @Size(min = 2, max = 20)
        String name,

        @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "올바른 전화번호 형식이 아닙니다.")
        String phone,

        @Email
        String email,

        District district,

        @Size(max = 50)
        String department,

        Team team,

        Position position
) {

    public UpdateMemberProfileAdminUseCase.Command toCommand() {
        return new UpdateMemberProfileAdminUseCase.Command(
                name,
                phone,
                email,
                district,
                department,
                team,
                position
        );
    }
}