package com.is.bcs.application.service;

import com.is.bcs.application.port.out.geo.CoordinateTransformer;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 조사 대상지 서식을 읽는다.
 *
 * 이 서식에는 점마다 조사 대상인지 적는 열(조사대상여부)이 있고, 그것이 기준점 서식과 구분되는 지점이다.
 * 다만 고객사가 열을 더하고 빼므로 없다고 거부하지는 않는다 — 다른 용도의 파일을 잘못 올렸는지 알아차릴 단서로만 쓴다.
 */
@Component
public class SurveyTargetMapper extends ImportFileMapper {

    public SurveyTargetMapper(CoordinateTransformer coordinateTransformer) {
        super(coordinateTransformer);
    }

    @Override
    protected List<String> requiredColumns() {
        return IDENTITY_COLUMNS;
    }

    @Override
    protected List<String> expectedColumns() {
        return List.of(TARGET_NOTE);
    }

    @Override
    protected List<String> foreignColumns() {
        return List.of();
    }
}
