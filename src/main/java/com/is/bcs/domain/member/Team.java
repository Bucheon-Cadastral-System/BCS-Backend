package com.is.bcs.domain.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Team {

    CIVIL_ADMINISTRATION("민원행정팀"),
    FAMILY_RELATION("가족관계팀"),
    CADASTRAL_INFORMATION("지적정보팀"),
    CADASTRAL_MANAGEMENT("지적관리팀"),
    REAL_ESTATE_MANAGEMENT("부동산관리팀");

    private final String displayName;
}