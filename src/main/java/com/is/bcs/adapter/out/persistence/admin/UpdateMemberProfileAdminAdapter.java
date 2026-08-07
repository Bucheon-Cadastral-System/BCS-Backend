package com.is.bcs.adapter.out.persistence.admin;

import com.is.bcs.adapter.out.persistence.member.MemberJpaEntity;
import com.is.bcs.adapter.out.persistence.member.MemberJpaRepository;
import com.is.bcs.application.port.in.admin.UpdateMemberProfileAdminUseCase;
import com.is.bcs.application.port.out.admin.UpdateMemberProfileAdminPort;
import com.is.bcs.domain.member.Member;
import com.is.bcs.domain.member.exception.DuplicateMemberEmailException;
import com.is.bcs.domain.member.exception.InvalidMemberProfileException;
import com.is.bcs.domain.member.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Locale;

@Repository
@RequiredArgsConstructor
public class UpdateMemberProfileAdminAdapter
        implements UpdateMemberProfileAdminPort {

    private final MemberJpaRepository memberJpaRepository;

    @Override
    public void updateProfile(Long memberId, UpdateMemberProfileAdminUseCase.Command command) {
        MemberJpaEntity entity = memberJpaRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다. memberId=" + memberId));

        validateEmail(entity, command.email());

        Member member = entity.toDomain();
        member.updateProfileByAdmin(
                command.name(),
                command.phone(),
                command.email(),
                command.district(),
                command.department(),
                command.team(),
                command.position()
        );

        try {
            memberJpaRepository.save(MemberJpaEntity.fromDomain(member));
            memberJpaRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateMemberEmailException("이미 다른 회원이 사용 중인 이메일입니다.", exception);
        }
    }

    private void validateEmail(MemberJpaEntity member, String email) {
        if (email == null) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (normalizedEmail.isBlank()) {
            throw new InvalidMemberProfileException("이메일은 공백일 수 없습니다.");
        }

        if (normalizedEmail.equalsIgnoreCase(member.getEmail())) {
            return;
        }

        if (memberJpaRepository.existsByEmailAndIdNot(normalizedEmail, member.getId())) {
            throw new DuplicateMemberEmailException("이미 다른 회원이 사용 중인 이메일입니다.");
        }
    }
}
