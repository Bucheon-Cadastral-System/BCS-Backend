package com.is.bcs.adapter.in.admin;

import com.is.bcs.application.port.out.member.SaveMemberPort;
import com.is.bcs.application.port.out.token.AccessTokenClaims;
import com.is.bcs.domain.member.District;
import com.is.bcs.domain.member.Member;
import com.is.bcs.domain.member.MemberRole;
import com.is.bcs.domain.member.Position;
import com.is.bcs.domain.member.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 어드민 회원 관리·활동 로그 API 계약 검증 — DB 필요(bcs/docker-compose). */
@SpringBootTest
@Transactional
class AdminApiTest {

    private static final OffsetDateTime AT = OffsetDateTime.parse("2026-08-01T10:00:00+09:00");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SaveMemberPort saveMemberPort;

    private MockMvc mockMvc;

    private int seq = 0;

    @BeforeEach
    void setUp() {
        // 컨트롤러가 Authentication 을 인자로 받으므로 시큐리티 필터를 태워야 주체가 실린다
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    /** 그 회원으로 로그인한 것처럼 요청한다 — 실제 토큰 발급 경로는 이 검증의 관심이 아니다. */
    private RequestPostProcessor as(long memberId) {
        AccessTokenClaims claims =
                new AccessTokenClaims(memberId, MemberRole.ADMIN, Instant.now(), Instant.now().plusSeconds(900));
        return authentication(new UsernamePasswordAuthenticationToken(claims, "n/a", List.of()));
    }

    private Member member(String name, boolean approved) {
        seq++;
        Member member = Member.registerWithKakao("kakao-admin-" + seq, AT);
        member.completeProfile(name, "0101234%04d".formatted(seq), "m" + seq + "@example.com",
                District.WONMI, "민원지적과", Team.CADASTRAL_MANAGEMENT, Position.OFFICER);
        if (approved) {
            member.approve(AT);
        }
        return saveMemberPort.save(member);
    }

    private String bodyOf(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String pick(String body, String field) {
        Matcher m = Pattern.compile("\"" + field + "\":\"?([^,\"}]+)").matcher(body);
        assertTrue(m.find(), field + " 가 응답에 없다: " + body);
        return m.group(1);
    }

    @Test
    @DisplayName("회원 목록을 상태·권한으로 걸러 정렬해 돌려준다")
    void getMembers_filtersAndSorts() throws Exception {
        member("김활성", true);
        member("박대기", false);

        MvcResult all = mockMvc.perform(get("/api/admin/members")
                        .param("sortBy", "name").param("direction", "ASC").with(as(member("관리자", true).getId())))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult pending = mockMvc.perform(get("/api/admin/members")
                        .param("status", "PENDING").with(as(member("관리자2", true).getId())))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(bodyOf(all).contains("김활성"));
        assertTrue(bodyOf(pending).contains("박대기"));
        // 걸러 보기가 실제로 좁히는지 — 활성 회원이 대기 목록에 섞이면 그 조건이 안 걸린 것이다
        assertFalse(bodyOf(pending).contains("김활성"));
    }

    @Test
    @DisplayName("정렬 기준·페이지 값이 지원 밖이면 400 이다 — 500 으로 새지 않는다")
    void getMembers_rejectsBadPaging() throws Exception {
        long adminId = member("관리자", true).getId();

        mockMvc.perform(get("/api/admin/members").param("sortBy", "phone").with(as(adminId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/members").param("page", "-1").with(as(adminId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/members").param("size", "0").with(as(adminId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/members").param("size", "101").with(as(adminId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("승인·거절·비활성화·활성화·승격·강등이 상태와 권한에 반영된다")
    void memberStateTransitions() throws Exception {
        long adminId = member("관리자", true).getId();
        long pendingId = member("박대기", false).getId();
        long rejectedId = member("최거절", false).getId();

        mockMvc.perform(patch("/api/admin/members/{id}/approve", pendingId).with(as(adminId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/admin/members/{id}/reject", rejectedId).with(as(adminId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/admin/members/{id}/deactivate", pendingId).with(as(adminId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/admin/members/{id}/activate", pendingId).with(as(adminId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/admin/members/{id}/role/admin", pendingId).with(as(adminId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/admin/members/{id}/role/user", pendingId).with(as(adminId)))
                .andExpect(status().isNoContent());

        String body = bodyOf(mockMvc.perform(get("/api/admin/members")
                        .param("status", "ACTIVE").param("size", "100").with(as(adminId)))
                .andExpect(status().isOk())
                .andReturn());
        assertTrue(body.contains("박대기"));
    }

    @Test
    @DisplayName("관리자가 회원 정보를 고치면 목록에 반영된다")
    void updateProfile() throws Exception {
        long adminId = member("관리자", true).getId();
        long targetId = member("김활성", true).getId();

        mockMvc.perform(patch("/api/admin/members/{id}/profile", targetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "김고침", "phone": "01098765432", "email": "fixed@example.com",
                                 "district": "SOSA", "team": "CADASTRAL_INFORMATION", "position": "TEAM_LEADER"}
                                """)
                        .with(as(adminId)))
                .andExpect(status().isNoContent());

        String body = bodyOf(mockMvc.perform(get("/api/admin/members")
                        .param("size", "100").with(as(adminId)))
                .andExpect(status().isOk())
                .andReturn());
        assertTrue(body.contains("김고침"));
        assertTrue(body.contains("fixed@example.com"));
    }

    @Test
    @DisplayName("활동 로그는 최신순으로 나오고 커서로 다음 쪽을 잇는다")
    void activities_paginateByCursor() throws Exception {
        long adminId = member("관리자", true).getId();
        long first = member("첫째", false).getId();
        long second = member("둘째", false).getId();
        mockMvc.perform(patch("/api/admin/members/{id}/approve", first).with(as(adminId)));
        mockMvc.perform(patch("/api/admin/members/{id}/approve", second).with(as(adminId)));

        String page = bodyOf(mockMvc.perform(get("/api/admin/activities")
                        .param("size", "1").with(as(adminId)))
                .andExpect(status().isOk())
                .andReturn());

        // 기록 시점의 이름이 함께 실린다 — 나중에 이름을 고쳐도 그때의 로그는 그대로여야 한다
        assertTrue(page.contains("MEMBER_APPROVED"));
        assertTrue(page.contains("\"actorName\":\"관리자\""));
        assertTrue(page.contains("\"hasNext\":true"));

        String cursor = pick(page, "nextCursor");
        assertNotNull(cursor);

        String next = bodyOf(mockMvc.perform(get("/api/admin/activities")
                        .param("size", "1").param("cursor", cursor).with(as(adminId)))
                .andExpect(status().isOk())
                .andReturn());
        // 이어 받은 쪽은 앞 쪽과 다른 줄이다 — 커서가 안 먹으면 같은 줄이 다시 온다
        assertFalse(next.equals(page));
    }

    @Test
    @DisplayName("활동 로그를 종류로 걸러 볼 수 있다")
    void activities_filterByType() throws Exception {
        long adminId = member("관리자", true).getId();
        long targetId = member("박대기", false).getId();
        mockMvc.perform(patch("/api/admin/members/{id}/approve", targetId).with(as(adminId)));

        String approved = bodyOf(mockMvc.perform(get("/api/admin/activities")
                        .param("activityType", "MEMBER_APPROVED").with(as(adminId)))
                .andExpect(status().isOk())
                .andReturn());
        String rejected = bodyOf(mockMvc.perform(get("/api/admin/activities")
                        .param("activityType", "MEMBER_REJECTED").with(as(adminId)))
                .andExpect(status().isOk())
                .andReturn());

        assertTrue(approved.contains("MEMBER_APPROVED"));
        assertFalse(rejected.contains("MEMBER_APPROVED"));
    }

    @Test
    @DisplayName("활동 로그의 잘못된 커서·크기는 400 이다")
    void activities_rejectsBadRequest() throws Exception {
        long adminId = member("관리자", true).getId();

        mockMvc.perform(get("/api/admin/activities").param("cursor", "!!!not-base64!!!").with(as(adminId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/activities").param("size", "0").with(as(adminId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/activities").param("size", "101").with(as(adminId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("내 정보 조회와 프로필 수정이 인증 주체를 따른다")
    void myProfile() throws Exception {
        long memberId = member("김활성", true).getId();

        String profile = bodyOf(mockMvc.perform(get("/api/members/me").with(as(memberId)))
                .andExpect(status().isOk())
                .andReturn());
        assertTrue(profile.contains("김활성"));

        mockMvc.perform(patch("/api/members/me/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone": "01055556666", "district": "OJEONG",
                                 "team": "CADASTRAL_INFORMATION", "position": "TEAM_LEADER"}
                                """)
                        .with(as(memberId)))
                .andExpect(status().isNoContent());

        String state = bodyOf(mockMvc.perform(get("/api/members/me/state").with(as(memberId)))
                .andExpect(status().isOk())
                .andReturn());
        assertEquals(true, state.contains("ACTIVE"));
    }
}
