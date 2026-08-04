package com.is.bcs.domain.controlpoint;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 기준점 표지 재질. */
@Getter
@RequiredArgsConstructor
public enum MarkerMaterial {

    STONE("표석"),
    STEEL("철재"),
    PLASTIC("플라스틱");

    private final String displayName;
}
