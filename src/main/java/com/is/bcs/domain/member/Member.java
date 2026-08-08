package com.is.bcs.domain.member;

import com.is.bcs.domain.member.exception.InvalidMemberInvariantException;
import com.is.bcs.domain.member.exception.InvalidMemberProfileException;
import com.is.bcs.domain.member.exception.InvalidMemberRoleException;
import com.is.bcs.domain.member.exception.InvalidMemberStateException;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Locale;

@Getter
public class Member {

    private final Long id;
    private final OAuthProvider provider;
    private final String providerUserId;

    private String name;
    private String phone;
    private String email;

    private District district;
    private String department;
    private Team team;
    private Position position;

    private MemberRole role;
    private MemberStatus status;

    private final OffsetDateTime requestedAt;
    private OffsetDateTime approvedAt;
    private OffsetDateTime deactivatedAt;

    private Member(
            Long id,
            OAuthProvider provider,
            String providerUserId,
            String name,
            String phone,
            String email,
            District district,
            String department,
            Team team,
            Position position,
            MemberRole role,
            MemberStatus status,
            OffsetDateTime requestedAt,
            OffsetDateTime approvedAt,
            OffsetDateTime deactivatedAt
    ) {
        this.id = id;
        this.provider = requireInvariant(provider, "OAuth 제공자");
        this.providerUserId = requireInvariantText(providerUserId, "OAuth 사용자 ID");
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.district = district;
        this.department = department;
        this.team = team;
        this.position = position;
        this.role = requireInvariant(role, "회원 권한");
        this.status = requireInvariant(status, "회원 상태");
        this.requestedAt = requireInvariant(requestedAt, "가입 요청 시각");
        this.approvedAt = approvedAt;
        this.deactivatedAt = deactivatedAt;
    }

    /**
     * 카카오 최초 로그인 회원 생성.
     *
     * 조직 정보는 이후 가입 정보 입력 과정에서 채운다.
     */
    public static Member registerWithKakao(
            String kakaoId,
            OffsetDateTime requestedAt
    ) {
        return new Member(
                null,
                OAuthProvider.KAKAO,
                kakaoId,
                null, // name
                null, // phone
                null, // email
                null, // district
                null, // department
                null, // team
                null, // position
                MemberRole.USER,
                MemberStatus.PENDING,
                requestedAt,
                null,
                null
        );
    }

    /**
     * DB 데이터를 도메인 객체로 복원한다.
     */
    public static Member restore(
            Long id,
            OAuthProvider provider,
            String providerUserId,
            String name,
            String phone,
            String email,
            District district,
            String department,
            Team team,
            Position position,
            MemberRole role,
            MemberStatus status,
            OffsetDateTime requestedAt,
            OffsetDateTime approvedAt,
            OffsetDateTime deactivatedAt
    ) {
        return new Member(
                id,
                provider,
                providerUserId,
                name,
                phone,
                email,
                district,
                department,
                team,
                position,
                role,
                status,
                requestedAt,
                approvedAt,
                deactivatedAt
        );
    }

    public void completeProfile(
            String name,
            String phone,
            String email,
            District district,
            String department,
            Team team,
            Position position
    ) {
        validatePendingStatus();

        // 검증을 모두 통과한 뒤에만 필드를 변경한다 — 중간 실패 시 부분 변경 방지
        String validName = requireText(name, "이름");
        String validPhone = requireText(phone, "전화번호");
        String validEmail = requireText(email, "이메일");
        District validDistrict = requireField(district, "지역");
        String validDepartment = requireText(department, "부서");
        Team validTeam = requireField(team, "팀");
        Position validPosition = requireField(position, "직급");

        this.name = validName;
        this.phone = validPhone;
        this.email = validEmail;
        this.district = validDistrict;
        this.department = validDepartment;
        this.team = validTeam;
        this.position = validPosition;
    }

    public void updateProfile(
            String phone,
            District district,
            Team team,
            Position position
    ){
        validateActiveStatus();

        String validPhone = requireText(phone, "전화번호");
        District validDistrict = requireField(district, "지역");
        Team validTeam = requireField(team, "팀");
        Position validPosition = requireField(position, "직급");

        this.phone = validPhone;
        this.district = validDistrict;
        this.team = validTeam;
        this.position = validPosition;
    }

    /**
     * 관리자가 회원 정보를 수정한다. 넘어온 값 중 null 이 아닌 항목만 반영한다.
     *
     * <p>null 은 고치지 않겠다는 뜻이고 공백 문자열은 값이 아니다. 공백을 그대로 받으면 활성 회원인데
     * 프로필은 미완성인 상태가 만들어진다.
     */
    public void updateProfileByAdmin(
            String name,
            String phone,
            String email,
            District district,
            String department,
            Team team,
            Position position
    ) {
        if (name != null) {
            this.name = requireText(name, "이름");
        }
        if (phone != null) {
            this.phone = requireText(phone, "전화번호");
        }
        if (email != null) {
            this.email = requireText(email, "이메일").toLowerCase(Locale.ROOT);
        }
        if (district != null) {
            this.district = district;
        }
        if (department != null) {
            this.department = requireText(department, "부서");
        }
        if (team != null) {
            this.team = team;
        }
        if (position != null) {
            this.position = position;
        }
    }


    public void approve(OffsetDateTime approvedAt) {
        validatePendingStatus();
        if (!isProfileCompleted()) {
            throw new InvalidMemberStateException("프로필이 완성되지 않아 승인할 수 없습니다.");
        }
        OffsetDateTime validApprovedAt = requireInvariant(approvedAt, "승인 시각");
        this.status = MemberStatus.ACTIVE;
        this.approvedAt = validApprovedAt;
        this.deactivatedAt = null;
    }

    public void reject(OffsetDateTime rejectedAt) {
        validatePendingStatus();
        OffsetDateTime validRejectedAt = requireInvariant(rejectedAt, "거절 시각");
        this.status = MemberStatus.INACTIVE;
        this.deactivatedAt = validRejectedAt;
        this.approvedAt = null;
    }

    public void deactivate(OffsetDateTime deactivatedAt) {
        if (status != MemberStatus.ACTIVE) {
            throw new InvalidMemberStateException("활성 회원만 비활성화할 수 있습니다.");
        }

        OffsetDateTime validApprovedAt = requireInvariant(deactivatedAt, "비활성화 시각");
        this.status = MemberStatus.INACTIVE;
        this.deactivatedAt = validApprovedAt;
    }

    /**
     * 비활성 회원을 다시 활성화한다.
     *
     * <p>승인과 같은 프로필 조건을 건다. 거절은 프로필이 덜 찬 가입 요청도 비활성으로 보내므로,
     * 상태만 보고 활성화하면 승인이 막았을 회원이 이 길로 들어온다.
     */
    public void activate(OffsetDateTime activatedAt) {
        validateInactiveStatus();
        if (!isProfileCompleted()) {
            throw new InvalidMemberStateException("프로필이 완성되지 않아 활성화할 수 없습니다.");
        }
        OffsetDateTime validActivatedAt = requireInvariant(activatedAt, "활성화 시각");

        this.status = MemberStatus.ACTIVE;
        this.deactivatedAt = null;

        // 거절 이력이 있어 승인 시각이 비어 있던 회원은 활성화 시각으로 채운다.
        if (this.approvedAt == null) {
            this.approvedAt = validActivatedAt;
        }
    }

    public void promoteToAdmin() {
        if (role != MemberRole.USER) {
            throw new InvalidMemberRoleException("USER 권한의 회원만 관리자로 승격할 수 있습니다.");
        }
        validateActiveStatus();

        this.role = MemberRole.ADMIN;
    }

    public void demoteToUser() {
        if (role != MemberRole.ADMIN) {
            throw new InvalidMemberRoleException("ADMIN 권한의 회원만 사용자로 강등할 수 있습니다.");
        }

        this.role = MemberRole.USER;
    }

    private void validatePendingStatus() {
        if (status != MemberStatus.PENDING) {
            throw new InvalidMemberStateException("승인 대기 상태에서만 처리할 수 있습니다.");
        }
    }

    private void validateActiveStatus() {
        if (status != MemberStatus.ACTIVE) {
            throw new InvalidMemberStateException("활성화된 상태에서만 처리할 수 있습니다.");
        }
    }

    private void validateInactiveStatus() {
        if (status != MemberStatus.INACTIVE) {
            throw new InvalidMemberStateException("비활성 회원만 다시 활성화할 수 있습니다.");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidMemberProfileException(fieldName + "은(는) 필수입니다.");
        }

        return value.trim();
    }

    private static <T> T requireField(T value, String fieldName) {
        if (value == null) {
            throw new InvalidMemberProfileException(fieldName + "은(는) 필수입니다.");
        }

        return value;
    }

    public boolean isProfileCompleted() {
        return hasText(name)
                && hasText(phone)
                && hasText(email)
                && district != null
                && hasText(department)
                && team != null
                && position != null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }


    private static String requireInvariantText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidMemberInvariantException(fieldName + "은(는) 필수입니다.");}

        return value.trim();
    }


    private static <T> T requireInvariant(T value, String fieldName) {

        if (value == null) {
            throw new InvalidMemberInvariantException(fieldName + "은(는) 필수입니다.");
        }

        return value;
    }

}