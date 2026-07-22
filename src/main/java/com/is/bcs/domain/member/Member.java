package com.is.bcs.domain.member;

import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Objects;

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
        this.provider = Objects.requireNonNull(provider);
        this.providerUserId = requireText(
                providerUserId,
                "OAuth 사용자 ID"
        );
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.district = district;
        this.department = department;
        this.team = team;
        this.position = position;
        this.role = Objects.requireNonNull(role);
        this.status = Objects.requireNonNull(status);
        this.requestedAt = Objects.requireNonNull(requestedAt);
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

        this.name = requireText(name, "이름");
        this.phone = requireText(phone, "전화번호");
        this.email = requireText(email, "이메일");
        this.district = Objects.requireNonNull(
                district,
                "지역은 필수입니다."
        );
        this.department = requireText(department, "부서");
        this.team = Objects.requireNonNull(
                team,
                "팀은 필수입니다."
        );
        this.position = Objects.requireNonNull(
                position,
                "직급은 필수입니다."
        );
    }

    public void approve(OffsetDateTime approvedAt) {
        validatePendingStatus();
        validateProfileCompleted();

        this.status = MemberStatus.ACTIVE;
        this.approvedAt = Objects.requireNonNull(approvedAt);
        this.deactivatedAt = null;
    }

    public void deactivate(OffsetDateTime deactivatedAt) {
        if (status != MemberStatus.ACTIVE) {
            throw new IllegalStateException(
                    "활성 회원만 비활성화할 수 있습니다."
            );
        }

        this.status = MemberStatus.INACTIVE;
        this.deactivatedAt =
                Objects.requireNonNull(deactivatedAt);
    }

    public void reactivate() {
        if (status != MemberStatus.INACTIVE) {
            throw new IllegalStateException(
                    "비활성 회원만 다시 활성화할 수 있습니다."
            );
        }

        this.status = MemberStatus.ACTIVE;
        this.deactivatedAt = null;
    }

    private void validatePendingStatus() {
        if (status != MemberStatus.PENDING) {
            throw new IllegalStateException(
                    "승인 대기 상태에서만 처리할 수 있습니다."
            );
        }
    }

    private void validateProfileCompleted() {
        requireText(name, "이름");
        requireText(phone, "전화번호");
        requireText(email, "이메일");
        Objects.requireNonNull(district, "지역은 필수입니다.");
        requireText(department, "부서");
        Objects.requireNonNull(team, "팀은 필수입니다.");
        Objects.requireNonNull(position, "직급은 필수입니다.");
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) 필수입니다."
            );
        }

        return value.trim();
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

}