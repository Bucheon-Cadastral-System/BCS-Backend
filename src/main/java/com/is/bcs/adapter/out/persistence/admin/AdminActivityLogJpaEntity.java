package com.is.bcs.adapter.out.persistence.admin;

import com.is.bcs.adapter.out.persistence.common.EntityReferences;
import com.is.bcs.adapter.out.persistence.member.MemberJpaEntity;
import com.is.bcs.domain.admin.AdminActivityType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "admin_activity_log",
        indexes = {
                @Index(
                        name = "idx_admin_activity_log_created_at_id",
                        columnList = "created_at, id"
                )
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminActivityLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 행위자 — 이름을 따로 보관하므로 회원이 남아 있는 한에서만 참조를 건다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_admin_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_admin_activity_logs_actor"))
    private MemberJpaEntity actorAdmin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_admin_activity_logs_target"))
    private MemberJpaEntity targetMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdminActivityType activityType;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private String actorName;

    @Column(nullable = false)
    private String targetName;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    private AdminActivityLogJpaEntity(
            MemberJpaEntity actorAdmin,
            MemberJpaEntity targetMember,
            AdminActivityType activityType,
            String message,
            String actorName,
            String targetName,
            OffsetDateTime createdAt
    ) {
        this.actorAdmin = actorAdmin;
        this.targetMember = targetMember;
        this.activityType = activityType;
        this.message = message;
        this.actorName = actorName;
        this.targetName = targetName;
        this.createdAt = createdAt;
    }

    public static AdminActivityLogJpaEntity of(
            Long actorAdminId, Long targetMemberId, AdminActivityType activityType,
            String message, String actorName, String targetName, OffsetDateTime createdAt,
            EntityManager entityManager
    ) {
        return new AdminActivityLogJpaEntity(
                EntityReferences.of(entityManager, MemberJpaEntity.class, actorAdminId),
                EntityReferences.of(entityManager, MemberJpaEntity.class, targetMemberId),
                activityType, message, actorName, targetName, createdAt);
    }
}