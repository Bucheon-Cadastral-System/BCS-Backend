package com.is.bcs.domain.controlpoint;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 지적기준점 종류 — 등급 순(삼각 > 삼각보조 > 도근). */
@Getter
@RequiredArgsConstructor
public enum PointType {

    TRIANGULATION("지적삼각점"),
    TRIANGULATION_AUX("지적삼각보조점"),
    DOGEUN("지적도근점");

    private final String displayName;
}
