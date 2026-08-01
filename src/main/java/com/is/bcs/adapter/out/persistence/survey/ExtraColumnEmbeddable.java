package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.domain.survey.ExtraColumn;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 조사 대상에 보관하는 열 하나. 열 이름을 컬럼으로 승격하지 않으므로 양식이 바뀌어도 스키마는 그대로다. */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExtraColumnEmbeddable {

    @Column(name = "column_name", nullable = false, length = 200)
    private String columnName;

    /** 뜻을 해석하지 않는 값이라 길이를 예상할 수 없다 — 자릿수를 걸면 긴 셀 하나에 임포트 전체가 막힌다. */
    @Column(name = "column_value", columnDefinition = "text")
    private String columnValue;

    private ExtraColumnEmbeddable(String columnName, String columnValue) {
        this.columnName = columnName;
        this.columnValue = columnValue;
    }

    public static ExtraColumnEmbeddable fromDomain(ExtraColumn extra) {
        return new ExtraColumnEmbeddable(extra.name(), extra.value());
    }

    public ExtraColumn toDomain() {
        return new ExtraColumn(columnName, columnValue);
    }
}
