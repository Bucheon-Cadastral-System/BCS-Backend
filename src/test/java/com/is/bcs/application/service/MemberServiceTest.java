package com.is.bcs.application.service;

import com.is.bcs.application.port.in.member.GetMyProfileUseCase;
import com.is.bcs.application.port.out.member.LoadMemberPort;
import com.is.bcs.application.port.out.member.SaveMemberPort;
import com.is.bcs.application.service.member.MemberService;
import com.is.bcs.domain.member.*;
import com.is.bcs.domain.member.exception.MemberNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberServiceTest {

    private final LoadMemberPort loadMemberPort = mock(LoadMemberPort.class);
    private final MemberService memberService =
            new MemberService(loadMemberPort, mock(SaveMemberPort.class));

    @Test
    void getProfileReturnsCurrentMemberProfile() {
        Member member = Member.restore(
                1L,
                OAuthProvider.KAKAO,
                "kakao-id",
                "홍길동",
                "01012345678",
                "hong@example.com",
                District.WONMI,
                "민원지적과",
                Team.CADASTRAL_MANAGEMENT,
                Position.OFFICER,
                MemberRole.USER,
                MemberStatus.ACTIVE,
                OffsetDateTime.parse("2026-07-01T10:00:00+09:00"),
                OffsetDateTime.parse("2026-07-02T10:00:00+09:00"),
                null
        );
        when(loadMemberPort.findById(1L)).thenReturn(Optional.of(member));

        GetMyProfileUseCase.Result result = memberService.getProfile(1L);

        assertEquals(1L, result.id());
        assertEquals("홍길동", result.name());
        assertEquals("01012345678", result.phone());
        assertEquals("hong@example.com", result.email());
        assertEquals(District.WONMI, result.district());
        assertEquals("민원지적과", result.department());
        assertEquals(Team.CADASTRAL_MANAGEMENT, result.team());
        assertEquals(Position.OFFICER, result.position());
        assertEquals(MemberRole.USER, result.role());
        assertEquals(MemberStatus.ACTIVE, result.status());
    }

    @Test
    void getProfileThrowsWhenMemberDoesNotExist() {
        when(loadMemberPort.findById(999L)).thenReturn(Optional.empty());

        assertThrows(MemberNotFoundException.class, () -> memberService.getProfile(999L));
    }
}
