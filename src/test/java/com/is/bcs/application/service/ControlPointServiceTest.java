package com.is.bcs.application.service;

import com.is.bcs.application.dto.RegisterControlPointCommand;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TraverseInfo;
import com.is.bcs.domain.controlpoint.exception.ControlPointNotFoundException;
import com.is.bcs.domain.controlpoint.exception.DuplicateControlPointException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ControlPointServiceTest {

    private final FakeControlPointStore store = new FakeControlPointStore();
    private final ControlPointService service = new ControlPointService(store, store);

    private static RegisterControlPointCommand csvRow1Command() {
        return new RegisterControlPointCommand(
                "41192D000001265", PointType.DOGEUN, "1465공",
                CoordinateSystem.GRS80_CENTRAL,
                new BigDecimal("545236.77"), new BigDecimal("181840.96"),
                126.794623, 37.506423,
                "10300", "춘의동", "경기도 부천시 춘의동 102-16",
                MarkerMaterial.STEEL, InstallType.INSTALLED, LocalDate.of(2018, 2, 21),
                new TraverseInfo("1", null, null, false)
        );
    }

    @Test
    @DisplayName("등록하면 id가 발급되고 성과·속성이 보존된 채 저장된다")
    void register_savesPoint() {
        ControlPoint saved = service.register(csvRow1Command());

        assertNotNull(saved.getId());
        assertEquals("41192D000001265", saved.getPointNo());
        assertEquals(CoordinateSystem.GRS80_CENTRAL, saved.getTm().crs());
        assertEquals(new BigDecimal("545236.77"), saved.getTm().northing());
        assertEquals(new BigDecimal("181840.96"), saved.getTm().easting());
        assertEquals(1, store.findAll().size());
    }

    @Test
    @DisplayName("이미 등록된 관리번호로는 등록할 수 없다")
    void register_duplicatePointNo_throws() {
        service.register(csvRow1Command());

        assertThrows(DuplicateControlPointException.class, () -> service.register(csvRow1Command()));
        assertEquals(1, store.findAll().size()); // 실패한 등록은 저장되지 않는다
    }

    @Test
    @DisplayName("공백이 섞인 관리번호도 정규화 후 중복으로 걸린다")
    void register_duplicateWithWhitespace_throws() {
        service.register(csvRow1Command());

        RegisterControlPointCommand padded = new RegisterControlPointCommand(
                "  41192D000001265  ", PointType.DOGEUN, "1465공",
                CoordinateSystem.GRS80_CENTRAL,
                new BigDecimal("545236.77"), new BigDecimal("181840.96"),
                126.794623, 37.506423,
                null, null, null, null, null, null, null
        );

        assertThrows(DuplicateControlPointException.class, () -> service.register(padded));
        assertEquals(1, store.findAll().size());
    }

    @Test
    @DisplayName("관리번호로 조회하고, 없으면 ControlPointNotFoundException")
    void getByPointNo() {
        service.register(csvRow1Command());

        assertEquals("1465공", service.getByPointNo("41192D000001265").getName());
        assertThrows(ControlPointNotFoundException.class, () -> service.getByPointNo("41192D999999999"));
    }

    @Test
    @DisplayName("전체 조회는 저장된 기준점 전부를 반환한다")
    void getAll_returnsAllSaved() {
        service.register(csvRow1Command());

        assertEquals(1, service.getAll().size());
    }

    /** 포트 페이크 — 인메모리 저장으로 서비스 로직만 검증한다. */
    private static class FakeControlPointStore implements LoadControlPointPort, SaveControlPointPort {

        private final Map<Long, ControlPoint> points = new HashMap<>();
        private long sequence = 0;

        @Override
        public Optional<ControlPoint> findById(Long id) {
            return Optional.ofNullable(points.get(id));
        }

        @Override
        public Optional<ControlPoint> findByPointNo(String pointNo) {
            return points.values().stream().filter(p -> p.getPointNo().equals(pointNo)).findFirst();
        }

        @Override
        public List<ControlPoint> findAll() {
            return new ArrayList<>(points.values());
        }

        @Override
        public boolean existsByPointNo(String pointNo) {
            return findByPointNo(pointNo).isPresent();
        }

        @Override
        public long count() {
            return points.size();
        }

        @Override
        public ControlPoint save(ControlPoint point) {
            long id = point.getId() != null ? point.getId() : ++sequence;
            ControlPoint saved = ControlPoint.restore(
                    id, point.getPointNo(), point.getType(), point.getName(),
                    point.getTm(), point.getGeo(),
                    point.getRegionCode(), point.getRegionName(), point.getAddress(),
                    point.getMarkerMaterial(), point.getInstallType(), point.getInstalledDate(),
                    point.getTraverse()
            );
            points.put(id, saved);
            return saved;
        }

        @Override
        public List<ControlPoint> saveAll(List<ControlPoint> list) {
            return list.stream().map(this::save).toList();
        }
    }
}
