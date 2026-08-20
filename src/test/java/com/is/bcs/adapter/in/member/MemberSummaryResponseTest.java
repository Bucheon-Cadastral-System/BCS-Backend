package com.is.bcs.adapter.in.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 남의 신원을 읽는 경로에 실리는 값을 못박는다.
 *
 * <p>인증만 하면 누구나 회원 번호를 바꿔 가며 물을 수 있는 경로라, 필드가 하나 늘면 그 값이 전 직원분
 * 열리는 것과 같다. 그래서 늘어나는 순간 이 시험이 깨지게 둔다.
 */
class MemberSummaryResponseTest {

    @Test
    @DisplayName("회원 신원 응답은 정해진 여덟 칸만 담는다")
    void allowedFields() {
        List<String> fields = Arrays.stream(MemberSummaryResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertEquals(List.of("id", "name", "phone", "email", "district", "department", "team", "position"), fields);
    }
}
