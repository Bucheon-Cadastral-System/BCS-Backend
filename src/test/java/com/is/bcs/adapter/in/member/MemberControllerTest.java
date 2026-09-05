package com.is.bcs.adapter.in.member;

import com.is.bcs.adapter.in.security.CurrentMemberIdResolver;
import com.is.bcs.application.port.in.member.CompleteMemberProfileUseCase;
import com.is.bcs.application.port.in.member.GetMemberStateUseCase;
import com.is.bcs.application.port.in.member.GetMyProfileUseCase;
import com.is.bcs.application.port.in.member.UpdateMemberProfileUseCase;
import com.is.bcs.application.port.out.token.AccessTokenClaims;
import com.is.bcs.domain.member.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberControllerTest {

    @Test
    void getMyProfileUsesMemberIdFromAccessTokenPrincipal() {
        GetMyProfileUseCase getMyProfileUseCase =
                mock(GetMyProfileUseCase.class);

        MemberController controller = new MemberController(
                mock(CompleteMemberProfileUseCase.class),
                mock(GetMemberStateUseCase.class),
                getMyProfileUseCase,
                mock(UpdateMemberProfileUseCase.class),
                mock(CurrentMemberIdResolver.class)
        );


        AccessTokenClaims claims = new AccessTokenClaims(
                7L,
                MemberRole.USER,
                Instant.parse("2026-07-30T00:00:00Z"),
                Instant.parse("2026-07-30T01:00:00Z")
        );
        when(getMyProfileUseCase.getProfile(7L)).thenReturn(profile(7L, true));

        var response = controller.getMyProfile(
                new UsernamePasswordAuthenticationToken(claims, null, List.of())
        );

        verify(getMyProfileUseCase).getProfile(7L);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(7L, response.getBody().id());
        assertEquals("홍길동", response.getBody().name());
        assertEquals("/api/members/7/profile-image", response.getBody().profileImageUrl());
    }

    @Test
    void getMemberSummaryCarriesImagePathOnlyForRegisteredImage() {
        GetMyProfileUseCase getMyProfileUseCase = mock(GetMyProfileUseCase.class);

        MemberController controller = new MemberController(
                mock(CompleteMemberProfileUseCase.class),
                mock(GetMemberStateUseCase.class),
                getMyProfileUseCase,
                mock(UpdateMemberProfileUseCase.class),
                mock(CurrentMemberIdResolver.class)
        );

        when(getMyProfileUseCase.getProfile(7L)).thenReturn(profile(7L, true));
        when(getMyProfileUseCase.getProfile(9L)).thenReturn(profile(9L, false));

        assertEquals("/api/members/7/profile-image", controller.getMemberSummary(7L).getBody().profileImageUrl());
        assertNull(controller.getMemberSummary(9L).getBody().profileImageUrl());
    }

    private static GetMyProfileUseCase.Result profile(Long memberId, boolean profileImageRegistered) {
        return new GetMyProfileUseCase.Result(
                memberId,
                "홍길동",
                "01012345678",
                "hong@example.com",
                District.WONMI,
                "민원지적과",
                Team.CADASTRAL_MANAGEMENT,
                Position.OFFICER,
                MemberRole.USER,
                MemberStatus.ACTIVE,
                profileImageRegistered
        );
    }
}
