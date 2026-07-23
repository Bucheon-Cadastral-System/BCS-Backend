package com.is.bcs.application.service;

import com.is.bcs.application.dto.OAuthLoginCommand;
import com.is.bcs.application.dto.OAuthLoginResult;
import com.is.bcs.application.port.out.member.LoadMemberPort;
import com.is.bcs.application.port.out.member.SaveMemberPort;
import com.is.bcs.application.service.oauth.OAuthLoginService;
import com.is.bcs.config.TimeConfig;
import com.is.bcs.domain.member.Member;
import com.is.bcs.domain.member.MemberRole;
import com.is.bcs.domain.member.MemberStatus;
import com.is.bcs.domain.member.OAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OAuthLoginServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-22T09:00:00Z");
    private static final OffsetDateTime FIXED_KST = OffsetDateTime.ofInstant(FIXED_INSTANT, TimeConfig.KST);

    private final Clock fixedClock = Clock.fixed(FIXED_INSTANT, TimeConfig.KST);
    private final FakeMemberStore store = new FakeMemberStore();
    private final OAuthLoginService service = new OAuthLoginService(store, store, fixedClock);

    @Test
    @DisplayName("미가입 카카오 사용자는 PENDING으로 저장되고 가입 요청 시각은 Clock(KST)을 따른다")
    void login_newUser_registersPending() {
        OAuthLoginResult result = service.login(new OAuthLoginCommand(OAuthProvider.KAKAO, "kakao-1"));

        assertEquals(MemberStatus.PENDING, result.status());
        assertEquals(MemberRole.USER, result.role());
        assertFalse(result.profileCompleted());
        assertEquals(1, store.saveCount);

        Member saved = store.findById(result.memberId()).orElseThrow();
        assertEquals("kakao-1", saved.getProviderUserId());
        assertEquals(FIXED_KST, saved.getRequestedAt());
    }

    @Test
    @DisplayName("기존 회원이 로그인하면 저장 없이 기존 상태를 반환한다")
    void login_existingUser_returnsWithoutSaving() {
        Member existing = store.save(Member.registerWithKakao("kakao-1", FIXED_KST));
        int savesBefore = store.saveCount;

        OAuthLoginResult result = service.login(new OAuthLoginCommand(OAuthProvider.KAKAO, "kakao-1"));

        assertEquals(existing.getId(), result.memberId());
        assertEquals(savesBefore, store.saveCount);
    }

    /** 포트 페이크 — 인메모리 저장으로 서비스 로직만 검증한다. */
    private static class FakeMemberStore implements LoadMemberPort, SaveMemberPort {

        private final Map<Long, Member> members = new HashMap<>();
        private long sequence = 0;
        int saveCount = 0;

        @Override
        public Optional<Member> findById(Long memberId) {
            return Optional.ofNullable(members.get(memberId));
        }

        @Override
        public Optional<Member> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId) {
            return members.values().stream()
                    .filter(m -> m.getProvider() == provider && m.getProviderUserId().equals(providerUserId))
                    .findFirst();
        }

        @Override
        public Member save(Member member) {
            saveCount++;
            long id = member.getId() != null ? member.getId() : ++sequence;
            Member saved = Member.restore(
                    id, member.getProvider(), member.getProviderUserId(),
                    member.getName(), member.getPhone(), member.getEmail(),
                    member.getDistrict(), member.getDepartment(), member.getTeam(), member.getPosition(),
                    member.getRole(), member.getStatus(),
                    member.getRequestedAt(), member.getApprovedAt(), member.getDeactivatedAt()
            );
            members.put(id, saved);
            return saved;
        }
    }
}
