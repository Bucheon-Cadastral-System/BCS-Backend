package com.is.bcs.domain.member;

import com.is.bcs.domain.member.exception.InvalidMemberInvariantException;
import com.is.bcs.domain.member.exception.InvalidMemberProfileException;
import com.is.bcs.domain.member.exception.InvalidMemberRoleException;
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
    @DisplayName("INACTIVE 회원을 활성화하면 ACTIVE가 되고 비활성화 시각이 지워진다")
    void activate_inactive_activates() {
        Member member = activeMember();
        member.deactivate(NOW.plusDays(2));

        member.activate(NOW.plusDays(3));

        assertEquals(MemberStatus.ACTIVE, member.getStatus());
        assertNull(member.getDeactivatedAt());
    }

    @Test
    @DisplayName("승인 시각이 없던 회원을 활성화하면 활성화 시각이 승인 시각을 채운다")
    void activate_withoutApprovedAt_backfillsApprovedAt() {
        Member member = completedPendingMember();
        member.reject(NOW.plusDays(1));
        OffsetDateTime activatedAt = NOW.plusDays(2);

        member.activate(activatedAt);

        assertEquals(MemberStatus.ACTIVE, member.getStatus());
        assertEquals(activatedAt, member.getApprovedAt());
        assertNull(member.getDeactivatedAt());
    }

    @Test
    @DisplayName("INACTIVE가 아닌 회원은 활성화할 수 없다")
    void activate_notInactive_throws() {
        assertThrows(InvalidMemberStateException.class, () -> activeMember().activate(NOW));
    }

    @Test
    @DisplayName("활성화 시각이 없으면 회원 불변식 예외가 발생한다")
    void activate_nullTime_throws() {
        Member member = activeMember();
        member.deactivate(NOW.plusDays(2));

        assertThrows(InvalidMemberInvariantException.class, () -> member.activate(null));
    }

    @Test
    @DisplayName("PENDING 회원을 거절하면 INACTIVE가 되고 승인 시각은 비워진다")
    void reject_pending_deactivates() {
        Member member = completedPendingMember();
        OffsetDateTime rejectedAt = NOW.plusDays(1);

        member.reject(rejectedAt);

        assertEquals(MemberStatus.INACTIVE, member.getStatus());
        assertEquals(rejectedAt, member.getDeactivatedAt());
        assertNull(member.getApprovedAt());
    }

    @Test
    @DisplayName("PENDING이 아닌 회원은 거절할 수 없다")
    void reject_notPending_throws() {
        assertThrows(InvalidMemberStateException.class, () -> activeMember().reject(NOW));
    }

    @Test
    @DisplayName("ACTIVE·USER 회원을 승격하면 ADMIN이 된다")
    void promoteToAdmin_activeUser_promotes() {
        Member member = activeMember();

        member.promoteToAdmin();

        assertEquals(MemberRole.ADMIN, member.getRole());
    }

    @Test
    @DisplayName("USER가 아닌 회원은 관리자로 승격할 수 없다")
    void promoteToAdmin_notUser_throws() {
        Member member = activeMember();
        member.promoteToAdmin();

        assertThrows(InvalidMemberRoleException.class, member::promoteToAdmin);
    }

    @Test
    @DisplayName("ACTIVE가 아닌 회원은 관리자로 승격할 수 없다")
    void promoteToAdmin_notActive_throws() {
        assertThrows(InvalidMemberStateException.class, () -> completedPendingMember().promoteToAdmin());
    }

    @Test
    @DisplayName("ADMIN 회원을 강등하면 USER가 된다")
    void demoteToUser_admin_demotes() {
        Member member = activeMember();
        member.promoteToAdmin();

        member.demoteToUser();

        assertEquals(MemberRole.USER, member.getRole());
    }

    @Test
    @DisplayName("ADMIN이 아닌 회원은 강등할 수 없다")
    void demoteToUser_notAdmin_throws() {
        assertThrows(InvalidMemberRoleException.class, () -> activeMember().demoteToUser());
    }

    @Test
    @DisplayName("관리자가 회원 정보를 수정하면 null이 아닌 값만 트림·정규화되어 반영된다")
    void updateProfileByAdmin_appliesOnlyNonNullFields() {
        Member member = activeMember();

        member.updateProfileByAdmin(
                " 김철수 ", null, " HONG@EXAMPLE.COM ",
                null, null, null, null
        );

        assertEquals("김철수", member.getName());
        assertEquals("hong@example.com", member.getEmail());
        assertEquals("010-1234-5678", member.getPhone());
    }
}
