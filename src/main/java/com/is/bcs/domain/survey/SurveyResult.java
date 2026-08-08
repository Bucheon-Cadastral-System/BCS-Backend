package com.is.bcs.domain.survey;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
}
