package com.is.bcs.adapter.out.persistence.member;

import com.is.bcs.application.port.out.member.LoadMemberNamesPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/** 회원 id → 이름 조회(읽기 전용) — 회원 축의 다른 코드를 건드리지 않는 별도 어댑터다. */
@Component
@RequiredArgsConstructor
public class MemberNameAdapter implements LoadMemberNamesPort {

    private final MemberJpaRepository memberRepository;

    @Override
    public Map<Long, String> findNamesByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return memberRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(MemberJpaEntity::getId, MemberJpaEntity::getName));
    }
}
