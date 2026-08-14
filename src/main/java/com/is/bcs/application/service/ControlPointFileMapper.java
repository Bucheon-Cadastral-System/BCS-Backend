package com.is.bcs.application.service;

import com.is.bcs.application.port.out.geo.CoordinateTransformer;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 기준점 서식을 읽는다.
 *
 * 기본 양식의 열을 요구한다 — 이 서식은 조사와 무관하게 기준점 자체를 적어 둔 것이라
 * 조사 대상 여부는 담지 않는다. 조사 대상 열이 섞여 있어도 읽되, 등록에는 쓰지 않는다.
 *
 * 이 서식에만 있는 열은 없어 빠진 열로는 다른 파일을 가려낼 수 없다.
 * 대신 조사 대상지에만 있는 열이 보이면 그 파일인지 알린다.
 */
@Component
public class ControlPointFileMapper extends ImportFileMapper {

    public ControlPointFileMapper(CoordinateTransformer coordinateTransformer) {
        super(coordinateTransformer);
    }

    @Override
    protected List<String> requiredColumns() {
        return BASE_COLUMNS;
    }

    @Override
    protected List<String> expectedColumns() {
        return List.of();
    }

    @Override
    protected List<String> foreignColumns() {
        return List.of(TARGET_NOTE);
    }
}
