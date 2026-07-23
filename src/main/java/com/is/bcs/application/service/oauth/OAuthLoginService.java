package com.is.bcs.application.service.oauth;

import com.is.bcs.application.dto.OAuthLoginCommand;
import com.is.bcs.application.dto.OAuthLoginResult;
import com.is.bcs.application.port.in.oauth.OAuthLoginUseCase;
import com.is.bcs.application.port.out.member.LoadMemberPort;
import com.is.bcs.application.port.out.member.SaveMemberPort;
import com.is.bcs.domain.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Transactional
public class OAuthLoginService implements OAuthLoginUseCase {

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final Clock clock;

    @Override
    public OAuthLoginResult login(OAuthLoginCommand command) {
        return loadMemberPort
                .findByProviderAndProviderUserId(
                        command.provider(),
                        command.providerUserId()
                )
                .map( member -> existingMemberResult(member) ) // 조회 결과 있으면 반환
                .orElseGet(() -> registerNewMember(command));  // 조회 결과 없으면 가입 처리
    }

    private OAuthLoginResult existingMemberResult(Member member) {
        return new OAuthLoginResult(
                member.getId(),
                member.getRole(),
                member.getStatus(),
                member.isProfileCompleted()
        );
    }

    private OAuthLoginResult registerNewMember(OAuthLoginCommand command) {
        // 신규 회원 객체 생성
        Member member = Member.registerWithKakao(command.providerUserId(), LocalDateTime.now(clock));

        Member savedMember = saveMemberPort.save(member);

        return new OAuthLoginResult(
                savedMember.getId(),
                savedMember.getRole(),
                savedMember.getStatus(),
                savedMember.isProfileCompleted()
        );
    }

}