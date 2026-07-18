//package com.is.bcs.application.service;
//
//import com.is.bcs.application.dto.OAuthLoginCommand;
//import com.is.bcs.application.dto.OAuthLoginResult;
//import com.is.bcs.application.port.in.OAuthLoginUseCase;
//import com.is.bcs.application.port.out.member.LoadMemberPort;
//import com.is.bcs.application.port.out.member.SaveMemberPort;
//import com.is.bcs.domain.member.Member;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class OAuthLoginService implements OAuthLoginUseCase {
//
//    private final LoadMemberPort loadMemberPort;
//    private final SaveMemberPort saveMemberPort;
//
//    @Override
//    public OAuthLoginResult login(OAuthLoginCommand command) {
//        Member member = loadMemberPort
//                .findByProviderAndProviderUserId(
//                        command.provider(),
//                        command.providerUserId()
//                )
//                .map(existingMember -> {
//                    existingMember.updateOAuthProfile(
//                            command.email(),
//                            command.nickname(),
//                            command.profileImageUrl()
//                    );
//                    return existingMember;
//                })
//                .orElseGet(() -> Member.registerOAuthMember(
//                        command.provider(),
//                        command.providerUserId(),
//                        command.email(),
//                        command.nickname(),
//                        command.profileImageUrl()
//                ));
//
//        Member savedMember = saveMemberPort.save(member);
//
//        return new OAuthLoginResult(
//                savedMember.getId(),
//                savedMember.getRole()
//        );
//    }
//}