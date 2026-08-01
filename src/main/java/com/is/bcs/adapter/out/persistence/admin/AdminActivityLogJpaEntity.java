package com.is.bcs.adapter.out.persistence.admin;

import com.is.bcs.domain.admin.AdminActivityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "admin_activity_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminActivityLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long actorAdminId;

    @Column(nullable = false)
    private Long targetMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdminActivityType activityType;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public AdminActivityLogJpaEntity(
            Long actorAdminId,
            Long targetMemberId,
            AdminActivityType activityType,
            String message,
            OffsetDateTime createdAt
    ) {
        this.actorAdminId = actorAdminId;
        this.targetMemberId = targetMemberId;
        this.activityType = activityType;
        this.message = message;
        this.createdAt = createdAt;
    }
}