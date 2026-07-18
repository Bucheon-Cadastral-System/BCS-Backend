package com.is.bcs.domain.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum District {

    WONMI("원미구"),
    SOSA("소사구"),
    OJEONG("오정구");

    private final String displayName;
}