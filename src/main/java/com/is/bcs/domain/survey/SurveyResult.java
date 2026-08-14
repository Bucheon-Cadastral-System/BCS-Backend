package com.is.bcs.domain.survey;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/**
 * 현장 조사 결과. 조사기록의 존재 자체가 '조사됨'을 뜻하고, 이 값은 그 판정이다.
 * 망실도 별도 상태 축이 아니라 조사 결과의 한 종류다.
 * 표시명은 화면이 쓰는 말과 같다. 파일 서식은 정상을 "완전"이라 적는데 그것은 읽는 자리에서 옮긴다.
 */
@Getter
@RequiredArgsConstructor
public enum SurveyResult {

    INTACT("정상"),
    LOST("망실"),
    /** 접근이 막혀 있는 등의 사유로 확인하지 못한 점 — 없어진 것이 아니므로 망실과 가른다. */
    UNAVAILABLE("조사불가"),
    ETC("기타");

    private final String displayName;

    /**
     * 표시명으로 되찾는다 — 기준점 마스터의 최종조사내용은 임포트가 이 이름으로 맞춰 저장한다
     * ({@code ImportFileMapper} 가 "완전"을 정상으로, "망실(포장)"을 망실로 옮긴다).
     *
     * <p>아는 말이 아니면 비어 있는 것으로 돌려준다. 임포트는 모르는 문구를 원문 그대로 두므로
     * 이 자리에는 어휘에 없는 값이 실재하고, 부르는 쪽이 그것을 무엇으로 셀지 정한다.
     */
    public static Optional<SurveyResult> fromDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return Optional.empty();
        }
        String trimmed = displayName.trim();
        return Arrays.stream(values()).filter(result -> result.displayName.equals(trimmed)).findFirst();
    }
}
