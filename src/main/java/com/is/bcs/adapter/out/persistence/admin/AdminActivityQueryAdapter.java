package com.is.bcs.adapter.out.persistence.admin;

import com.is.bcs.application.port.in.admin.GetAdminActivityUseCase;
import com.is.bcs.application.port.out.admin.GetAdminActivityPort;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AdminActivityQueryAdapter implements GetAdminActivityPort {

    private static final QAdminActivityLogJpaEntity activityLog = QAdminActivityLogJpaEntity.adminActivityLogJpaEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<GetAdminActivityUseCase.Result> findActivities(
            Pageable pageable,
            GetAdminActivityUseCase.Command command
    ) {
        // 1. 활동 타입 동적 처리
        BooleanBuilder conditions = createConditions(command);

        // 2. 활동 로그 페이징 조회
        List<GetAdminActivityUseCase.Result> content = queryFactory
                .selectFrom(activityLog)
                .where(conditions)
                .orderBy(
                        activityLog.createdAt.desc(),
                        activityLog.id.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch()
                .stream()
                .map(this::toResult)
                .toList();

        // 3. 전체 조회 수
        Long total = queryFactory
                .select(activityLog.count())
                .from(activityLog)
                .where(conditions)
                .fetchOne();

        // 4. 반환
        return new PageImpl<>(
                content,
                pageable,
                total == null ? 0L : total
        );
    }

    private BooleanBuilder createConditions(GetAdminActivityUseCase.Command command) {
        BooleanBuilder builder = new BooleanBuilder();

        if (command.activityType() != null) {
            builder.and(activityLog.activityType.eq(command.activityType()));
        }

        return builder;
    }

    private GetAdminActivityUseCase.Result toResult(
            AdminActivityLogJpaEntity entity
    ) {
        return new GetAdminActivityUseCase.Result(
                entity.getId(),
                entity.getActorAdminId(),
                entity.getTargetMemberId(),
                entity.getActivityType(),
                entity.getMessage(),
                entity.getCreatedAt()
        );
    }
}