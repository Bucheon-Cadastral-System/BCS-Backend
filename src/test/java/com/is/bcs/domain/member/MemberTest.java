package com.is.bcs.domain.member;

import com.is.bcs.domain.member.exception.InvalidMemberInvariantException;
import com.is.bcs.domain.member.exception.InvalidMemberProfileException;
import com.is.bcs.domain.member.exception.InvalidMemberStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-22T18:00:00+09:00");

    private static Member pendingMember() {
        return Member.registerWithKakao("kakao-123", NOW);
    }

    private static Member completedPendingMember() {
        Member member = pendingMember();
        member.completeProfile(
                "홍길동", "010-1234-5678", "hong@bucheon.go.kr",
                District.WONMI, "토지정보과", Team.CADASTRAL_INFORMATION, Position.OFFICER
        );
        return member;
    }

    private static Member activeMember() {
        Member member = completedPendingMember();
        member.approve(NOW.plusDays(1));
        return member;
    }

    @Test
    @DisplayName("카카오 최초 가입은 PENDING·USER·프로필 미완성 상태로 생성된다")
    void registerWithKakao_createsPendingUser() {
        Member member = pendingMember();

        assertEquals(OAuthProvider.KAKAO, member.getProvider());
        assertEquals("kakao-123", member.getProviderUserId());
        assertEquals(MemberStatus.PENDING, member.getStatus());
        assertEquals(MemberRole.USER, member.getRole());
        assertEquals(NOW, member.getRequestedAt());
        assertFalse(member.isProfileCompleted());
    }



    @Test
    @DisplayName("providerUserId가 비어 있으면 회원 불변식 예외가 발생한다")
    void register_blankProviderUserId_throws() {
        assertThrows(InvalidMemberInvariantException.class, () -> Member.registerWithKakao(" ", NOW));
        assertThrows(InvalidMemberInvariantException.class, () -> Member.registerWithKakao(null, NOW));
    }

    @Test
    @DisplayName("프로필을 완성하면 값이 트림되어 저장되고 완성 상태가 된다")
    void completeProfile_setsTrimmedFields() {
        Member member = pendingMember();

        member.completeProfile(
                "  홍길동  ", " 010-1234-5678 ", " hong@bucheon.go.kr ",
                District.WONMI, " 토지정보과 ", Team.CADASTRAL_INFORMATION, Position.OFFICER
        );

        assertEquals("홍길동", member.getName());
        assertEquals("010-1234-5678", member.getPhone());
        assertEquals("hong@bucheon.go.kr", member.getEmail());
        assertEquals("토지정보과", member.getDepartment());
        assertTrue(member.isProfileCompleted());
    }

    @Test
    @DisplayName("프로필 필수값이 비어 있으면 InvalidMemberProfileException")
    void completeProfile_blankField_throws() {
        assertThrows(InvalidMemberProfileException.class, () -> pendingMember().completeProfile(
                " ", "010-1234-5678", "hong@bucheon.go.kr",
                District.WONMI, "토지정보과", Team.CADASTRAL_INFORMATION, Position.OFFICER
        ));
        assertThrows(InvalidMemberProfileException.class, () -> pendingMember().completeProfile(
                "홍길동", "010-1234-5678", "hong@bucheon.go.kr",
                null, "토지정보과", Team.CADASTRAL_INFORMATION, Position.OFFICER
        ));
    }

    @Test
    @DisplayName("프로필 검증이 중간에 실패하면 앞선 필드도 변경되지 않는다")
    void completeProfile_failure_noPartialMutation() {
        Member member = pendingMember();

        assertThrows(InvalidMemberProfileException.class, () -> member.completeProfile(
                "홍길동", "010-1234-5678", "hong@bucheon.go.kr",
                District.WONMI, "토지정보과", Team.CADASTRAL_INFORMATION, null // 마지막 필드만 위반
        ));

        assertNull(member.getName());
        assertFalse(member.isProfileCompleted());
    }

    @Test
    @DisplayName("승인·비활성화 시각이 없으면 회원 불변식 예외가 발생하고 상태가 유지된다")
    void stateTransition_nullTime_noStateChange() {
        Member pending = completedPendingMember();
        assertThrows(InvalidMemberInvariantException.class, () -> pending.approve(null));
        assertEquals(MemberStatus.PENDING, pending.getStatus());
        assertNull(pending.getApprovedAt());

        Member active = activeMember();
        assertThrows(InvalidMemberInvariantException.class, () -> active.deactivate(null));
        assertEquals(MemberStatus.ACTIVE, active.getStatus());
        assertNull(active.getDeactivatedAt());
    }

    @Test
    @DisplayName("PENDING이 아닌 회원은 프로필을 완성할 수 없다")
    void completeProfile_notPending_throws() {
        Member active = activeMember();

        assertThrows(InvalidMemberStateException.class, () -> active.completeProfile(
                "홍길동", "010-1234-5678", "hong@bucheon.go.kr",
                District.WONMI, "토지정보과", Team.CADASTRAL_INFORMATION, Position.OFFICER
        ));
    }

    @Test
    @DisplayName("프로필이 완성된 PENDING 회원을 승인하면 ACTIVE가 된다")
    void approve_completedPending_activates() {
        Member member = completedPendingMember();
        OffsetDateTime approvedAt = NOW.plusDays(1);

        member.approve(approvedAt);

        assertEquals(MemberStatus.ACTIVE, member.getStatus());
        assertEquals(approvedAt, member.getApprovedAt());
        assertNull(member.getDeactivatedAt());
    }

    @Test
    @DisplayName("프로필이 미완성이면 승인할 수 없다")
    void approve_incompleteProfile_throws() {
        assertThrows(InvalidMemberStateException.class, () -> pendingMember().approve(NOW));
    }

    @Test
    @DisplayName("PENDING이 아닌 회원은 승인할 수 없다")
    void approve_notPending_throws() {
        assertThrows(InvalidMemberStateException.class, () -> activeMember().approve(NOW));
    }

    @Test
    @DisplayName("ACTIVE 회원을 비활성화하면 INACTIVE와 비활성화 시각이 기록된다")
    void deactivate_active_deactivates() {
        Member member = activeMember();
        OffsetDateTime deactivatedAt = NOW.plusDays(2);

        member.deactivate(deactivatedAt);

        assertEquals(MemberStatus.INACTIVE, member.getStatus());
        assertEquals(deactivatedAt, member.getDeactivatedAt());
    }

    @Test
    @DisplayName("ACTIVE가 아닌 회원은 비활성화할 수 없다")
    void deactivate_notActive_throws() {
        assertThrows(InvalidMemberStateException.class, () -> pendingMember().deactivate(NOW));
    }

    @Test
    @DisplayName("INACTIVE 회원을 재활성화하면 ACTIVE가 되고 비활성화 시각이 지워진다")
    void reactivate_inactive_activates() {
        Member member = activeMember();
        member.deactivate(NOW.plusDays(2));

        member.reactivate();

        assertEquals(MemberStatus.ACTIVE, member.getStatus());
        assertNull(member.getDeactivatedAt());
    }

    @Test
    @DisplayName("INACTIVE가 아닌 회원은 재활성화할 수 없다")
    void reactivate_notInactive_throws() {
        assertThrows(InvalidMemberStateException.class, () -> activeMember().reactivate());
    }
}
