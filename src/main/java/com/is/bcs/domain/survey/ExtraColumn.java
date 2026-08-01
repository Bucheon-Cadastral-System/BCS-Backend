package com.is.bcs.domain.survey;

import java.util.Objects;

/**
 * 대상지 파일에서 기본 양식에 없던 열 — 이름과 값을 적힌 그대로 보관한다.
 * 뜻을 해석하지 않으므로 고객사가 열을 더하거나 이름을 바꿔도 코드가 따라갈 필요가 없다.
 *
 * @param value 빈 칸일 수 있다 — 값이 없어도 그 열이 파일에 있었다는 사실은 남긴다.
 */
public record ExtraColumn(String name, String value) {

    public ExtraColumn {
        Objects.requireNonNull(name, "열 이름은 필수입니다.");
    }
}
