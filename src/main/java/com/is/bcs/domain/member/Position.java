package com.is.bcs.domain.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Position {

    TEAM_LEADER("팀장"),
    OFFICER("주무관");

    private final String displayName;
}