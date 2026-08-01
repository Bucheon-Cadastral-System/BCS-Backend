package com.is.bcs.application.service.admin;

import com.is.bcs.application.port.in.admin.UpdateMemberProfileAdminUseCase;
import com.is.bcs.application.port.out.admin.UpdateMemberProfileAdminPort;
import com.is.bcs.domain.member.exception.InvalidMemberProfileException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateMemberProfileAdminService
        implements UpdateMemberProfileAdminUseCase {

    private final UpdateMemberProfileAdminPort updateMemberProfileAdminPort;

    @Override
    public void updateProfile(Long memberId, Command command) {
        if (!command.hasChanges()) {
            throw new InvalidMemberProfileException("변경할 회원 정보를 한 개 이상 입력해야 합니다.");
        }

        updateMemberProfileAdminPort.updateProfile(memberId, command);
    }
}