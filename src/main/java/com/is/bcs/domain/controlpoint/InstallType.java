package com.is.bcs.domain.controlpoint;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 설치구분 — 최초 설치와 재설치(망실·훼손 후 다시 설치)를 구분한다. 그 밖의 경위는 기타로 둔다. */
@Getter
@RequiredArgsConstructor
public enum InstallType {

    INSTALLED("설치"),
    REINSTALLED("재설치"),
    ETC("기타");

    private final String displayName;
}
