package com.is.bcs.application.service.member;

import com.is.bcs.application.port.in.member.CompleteMemberProfileUseCase;
import com.is.bcs.application.port.in.member.GetMemberStateUseCase;
import com.is.bcs.application.port.out.member.LoadMemberPort;
import com.is.bcs.application.port.out.member.SaveMemberPort;
import com.is.bcs.domain.member.Member;
import com.is.bcs.domain.member.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService
        implements CompleteMemberProfileUseCase, GetMemberStateUseCase {

    private static final String DEPARTMENT = "민원지적과";

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;

    @Override
    @Transactional
    public void complete(Long memberId, Command command) {
        Member member = getMember(memberId);

        member.completeProfile(
                command.name(),
                command.phone(),
                command.email(),
                command.district(),
                DEPARTMENT,
                command.team(),
                command.position()
        );

        saveMemberPort.save(member);
    }

    @Override
    @Transactional(readOnly = true)
    public Result getState(Long memberId) {
        Member member = getMember(memberId);

        return new Result(
                member.getStatus(),
                member.isProfileCompleted()
        );
    }

    private Member getMember(Long memberId) {
        return loadMemberPort.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException( "회원을 찾을 수 없습니다. memberId=" + memberId));
    }
}