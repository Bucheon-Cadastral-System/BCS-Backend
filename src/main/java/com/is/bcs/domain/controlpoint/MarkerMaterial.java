package com.is.bcs.domain.controlpoint;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 기준점 표지 재질. */
@Getter
@RequiredArgsConstructor
public enum MarkerMaterial {

    STONE("표석"),
    STEEL("철재");

    private final String displayName;
}
