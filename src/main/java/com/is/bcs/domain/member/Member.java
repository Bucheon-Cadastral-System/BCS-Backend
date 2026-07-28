package com.is.bcs.domain.member;

import com.is.bcs.domain.member.exception.InvalidMemberInvariantException;
import com.is.bcs.domain.member.exception.InvalidMemberProfileException;
import com.is.bcs.domain.member.exception.InvalidMemberStateException;
import lombok.Getter;

import java.time.OffsetDateTime;

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

    public void deactivate(OffsetDateTime deactivatedAt) {
        if (status != MemberStatus.ACTIVE) {
            throw new InvalidMemberStateException("활성 회원만 비활성화할 수 있습니다.");
        }

        OffsetDateTime validApprovedAt = requireInvariant(deactivatedAt, "비활성화 시각");
        this.status = MemberStatus.INACTIVE;
        this.deactivatedAt = validApprovedAt;
    }

    public void reactivate() {
        if (status != MemberStatus.INACTIVE) {
            throw new InvalidMemberStateException("비활성 회원만 다시 활성화할 수 있습니다.");
        }

        this.status = MemberStatus.ACTIVE;
        this.deactivatedAt = null;
    }

    private void validatePendingStatus() {
        if (status != MemberStatus.PENDING) {
            throw new InvalidMemberStateException("승인 대기 상태에서만 처리할 수 있습니다.");
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