package com.is.bcs.adapter.out.persistence.admin;

import com.is.bcs.adapter.in.web.exception.InvalidPageRequestException;
import com.is.bcs.adapter.out.persistence.member.MemberJpaEntity;
import com.is.bcs.application.port.in.admin.GetMemberAdminUseCase;
import com.is.bcs.application.port.out.admin.GetMemberAdminPort;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.is.bcs.adapter.out.persistence.member.QMemberJpaEntity.memberJpaEntity;

@Repository
@RequiredArgsConstructor
public class MemberAdminQueryAdapter implements GetMemberAdminPort {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<GetMemberAdminUseCase.Result> findMembers(Pageable pageable, GetMemberAdminUseCase.Command command) {
        // 1. 동적 쿼리 모으기
        BooleanBuilder conditions = createConditions(command);

        // 2. WHERE 절에 동적쿼리들로 DB-Member 조회
        JPAQuery<MemberJpaEntity> contentQuery = queryFactory
                .selectFrom(memberJpaEntity)
                .where(conditions)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        // 3. 정렬하기
        applySorting(contentQuery, pageable);

        // 4. 정렬된 Member로 DTO변환
        List<GetMemberAdminUseCase.Result> content = contentQuery
                .fetch()
                .stream()
                .map(this::toResult)
                .toList();

        // 5. 조회된 회원 수
        Long total = queryFactory
                .select(memberJpaEntity.count())
                .from(memberJpaEntity)
                .where(conditions)
                .fetchOne();

        // 6. 페이징 반환
        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    private GetMemberAdminUseCase.Result toResult(MemberJpaEntity entity) {
        return new GetMemberAdminUseCase.Result(
                entity.getId(),
                entity.getName(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getDistrict(),
                entity.getDepartment(),
                entity.getTeam(),
                entity.getPosition(),
                entity.getStatus(),
                entity.getRole()
        );
    }

    private BooleanBuilder createConditions(GetMemberAdminUseCase.Command command) {
        BooleanBuilder builder = new BooleanBuilder();

        if (hasText(command.name())) {
            builder.and(memberJpaEntity.name.containsIgnoreCase(command.name().trim()));
        }

        if (hasText(command.email())) {
            builder.and(memberJpaEntity.email.containsIgnoreCase(command.email().trim()));
        }

        if (hasText(command.phone())) {
            builder.and(memberJpaEntity.phone.containsIgnoreCase(command.phone().trim()));
        }

        if (command.district() != null) {
            builder.and(memberJpaEntity.district.eq(command.district()));
        }

        if (command.team() != null) {
            builder.and(memberJpaEntity.team.eq(command.team()));
        }

        if (command.position() != null) {
            builder.and(memberJpaEntity.position.eq(command.position()));
        }

        if (command.status() != null) {
            builder.and(memberJpaEntity.status.eq(command.status()));
        }

        if (command.role() != null) {
            builder.and(memberJpaEntity.role.eq(command.role()));
        }

        return builder;
    }

    private void applySorting(JPAQuery<MemberJpaEntity> query, Pageable pageable) {
        for (Sort.Order sortOrder : pageable.getSort()) {
            ComparableExpressionBase<?> sortExpression = getSortExpression(sortOrder.getProperty());

            Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;

            query.orderBy(new OrderSpecifier<>(direction, sortExpression));
        }

        // 정렬값이 동일한 회원들의 순서를 고정한다.
        query.orderBy(memberJpaEntity.id.desc());
    }

    private ComparableExpressionBase<?> getSortExpression(String property) {
        return switch (property) {
            case "name" -> memberJpaEntity.name;
            case "email" -> memberJpaEntity.email;
            case "district" -> memberJpaEntity.district;
            case "team" -> memberJpaEntity.team;
            case "position" -> memberJpaEntity.position;
            case "status" -> memberJpaEntity.status;
            case "role" -> memberJpaEntity.role;
            case "createdAt" -> memberJpaEntity.createdAt;
            default -> throw new InvalidPageRequestException("지원하지 않는 정렬 기준입니다: " + property);
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}