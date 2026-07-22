package com.is.bcs.adapter.out.persistence.member;

import com.is.bcs.adapter.out.persistence.common.BaseTime;
import com.is.bcs.domain.member.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(
        name = "members",
        schema = "bcs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_members_oauth", columnNames = {"oauth_provider", "provider_user_id"})
        },
        indexes = {
                @Index(name = "idx_members_status", columnList = "status"),
                @Index(name = "idx_members_requested_at", columnList = "requested_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberJpaEntity extends BaseTime {
    // requested_at(가입 신청 시각)은 업무 필드라 도메인이 소유하고,
    // created_at/updated_at(BaseTime)은 감사용 인프라 컬럼이라 도메인에 매핑하지 않는다.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "oauth_provider",
            nullable = false,
            length = 20
    )
    private OAuthProvider provider;

    @Column(
            name = "provider_user_id",
            nullable = false,
            length = 100
    )
    private String providerUserId;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "district", length = 30)
    private District district;

    @Column(name = "department", length = 100)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "team", length = 50)
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", length = 30)
    private Position position;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "role",
            nullable = false,
            length = 20
    )
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private MemberStatus status;

    @Column(
            name = "requested_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime requestedAt;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "deactivated_at")
    private OffsetDateTime deactivatedAt;

    private MemberJpaEntity(
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
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.district = district;
        this.department = department;
        this.team = team;
        this.position = position;
        this.role = role;
        this.status = status;
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
        this.deactivatedAt = deactivatedAt;
    }

    public static MemberJpaEntity fromDomain(Member member) {
        return new MemberJpaEntity(
                member.getId(),
                member.getProvider(),
                member.getProviderUserId(),
                member.getName(),
                member.getPhone(),
                member.getEmail(),
                member.getDistrict(),
                member.getDepartment(),
                member.getTeam(),
                member.getPosition(),
                member.getRole(),
                member.getStatus(),
                member.getRequestedAt(),
                member.getApprovedAt(),
                member.getDeactivatedAt()
        );
    }

    public Member toDomain() {
        return Member.restore(
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
}