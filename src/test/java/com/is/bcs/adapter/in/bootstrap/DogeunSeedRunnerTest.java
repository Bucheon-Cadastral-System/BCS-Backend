package com.is.bcs.adapter.in.bootstrap;

import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.PointType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DogeunSeedRunnerTest {

    @Test
    @DisplayName("기준점이 하나도 없으면 시드 2,146점을 등록한다")
    void run_emptyTable_seeds() throws Exception {
        FakeControlPointStore store = new FakeControlPointStore();

        new DogeunSeedRunner(store, store).run();

        assertEquals(2146, store.saved.size());
    }

    @Test
    @DisplayName("기준점이 이미 있으면 아무것도 등록하지 않는다")
    void run_nonEmptyTable_skips() throws Exception {
        FakeControlPointStore store = new FakeControlPointStore();
        store.saved.addAll(DogeunSeedCsv.load().subList(0, 1));

        new DogeunSeedRunner(store, store).run();

        assertEquals(1, store.saved.size());
    }

    /** 포트 페이크 — 저장 목록만 기록한다. */
    private static class FakeControlPointStore implements LoadControlPointPort, SaveControlPointPort {

        final List<ControlPoint> saved = new ArrayList<>();

        @Override
        public Optional<ControlPoint> findById(Long id) {
            return Optional.empty();
        }

        @Override
        public Optional<ControlPoint> findByPointNo(String pointNo) {
            return Optional.empty();
        }

        @Override
        public List<ControlPoint> findAll() {
            return new ArrayList<>(saved);
        }

        @Override
        public boolean existsByPointNo(String pointNo) {
            return false;
        }

        @Override
        public long count() {
            return saved.size();
        }

        @Override
        public Map<PointType, Long> countByType() {
            Map<PointType, Long> counts = new HashMap<>();
            saved.forEach(p -> counts.merge(p.getType(), 1L, Long::sum));
            return counts;
        }

        @Override
        public ControlPoint save(ControlPoint point) {
            saved.add(point);
            return point;
        }

        @Override
        public List<ControlPoint> saveAll(List<ControlPoint> points) {
            saved.addAll(points);
            return points;
        }
    }
}
