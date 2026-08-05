package com.is.bcs.application.port.out.member;

import java.util.Collection;
import java.util.Map;

/** 회원 id → 표시 이름 조회(읽기 전용) — 목록의 작성자 표기처럼 이름만 필요한 자리에서 쓴다. */
public interface LoadMemberNamesPort {

    Map<Long, String> findNamesByIds(Collection<Long> ids);
}
