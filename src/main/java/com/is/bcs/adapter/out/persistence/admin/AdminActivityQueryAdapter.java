package com.is.bcs.adapter.out.persistence.admin;

import com.is.bcs.application.port.in.admin.GetAdminActivityUseCase;
import com.is.bcs.application.port.out.admin.GetAdminActivityPort;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AdminActivityQueryAdapter implements GetAdminActivityPort {

    private static final QAdminActivityLogJpaEntity activityLog = QAdminActivityLogJpaEntity.adminActivityLogJpaEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<GetAdminActivityUseCase.Result> findActivities(Pageable pageable, GetAdminActivityUseCase.Command command) {

        // 1. 동적 쿼리 처리
        BooleanBuilder conditions = createConditions(command);

        // 2. 로그 동적 조회
        List<AdminActivityLogJpaEntity> rows = queryFactory
                .selectFrom(activityLog)
                .where(conditions)
                .orderBy(
                        activityLog.createdAt.desc(),
                        activityLog.id.desc()
                )
                .limit(pageable.getPageSize() + 1L)
                .fetch();

        // 3. 다음 페이지 유/무 화인
        boolean hasNext = rows.size() > pageable.getPageSize();

        // 4. 다음이 있는 경우와 없는 경우
        if (hasNext) {
            rows = rows.subList(0, pageable.getPageSize());
        }

        // 5. DTO 변환
        List<GetAdminActivityUseCase.Result> content = rows.stream()
                .map(this::toResult)
                .toList();

        // 6. 반환
        return new SliceImpl<>(content, pageable, hasNext);
    }

    private BooleanBuilder createConditions(GetAdminActivityUseCase.Command command) {
        BooleanBuilder builder = new BooleanBuilder();

        if (command.activityType() != null) {
            builder.and(
                    activityLog.activityType.eq(command.activityType())
            );
        }

        builder.and(
                cursorCondition(
                        command.cursorCreatedAt(),
                        command.cursorId()
                )
        );

        return builder;
    }

    private BooleanExpression cursorCondition(OffsetDateTime cursorCreatedAt, Long cursorId) {
        if (cursorCreatedAt == null || cursorId == null) {
            return null;
        }

        return activityLog.createdAt.lt(cursorCreatedAt)
                .or(
                        activityLog.createdAt.eq(cursorCreatedAt)
                                .and(activityLog.id.lt(cursorId))
                );
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
                entity.getActorName(),
                entity.getTargetName(),
                entity.getCreatedAt()
        );
    }
}