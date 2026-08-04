package com.is.bcs.adapter.out.persistence.controlpoint;

import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpoint.TraverseInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 기준점 영속 왕복 검증 — DB 필요(bcs/docker-compose). 기대값은 고객사 대상지 CSV 실데이터. */
@SpringBootTest
@Transactional
class ControlPointPersistenceAdapterTest {

    @Autowired
    private ControlPointPersistenceAdapter adapter;

    @Autowired
    private ControlPointJpaRepository repository;

    private static ControlPoint csvRow1() {
        return ControlPoint.register(
                "41192D000001265", PointType.DOGEUN, "1465공",
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                        new BigDecimal("545236.77"), new BigDecimal("181840.96")),
                new GeoCoordinate(126.794623, 37.506423),
                "10300", "춘의동", "경기도 부천시 춘의동 102-16",
                MarkerMaterial.STEEL, InstallType.INSTALLED, LocalDate.of(2018, 2, 21),
                new TraverseInfo("1", null, null, false)
        , null, null, null);
    }

    @Test
    @DisplayName("성과 좌표는 DB 왕복 후에도 소수 4자리 스케일까지 보존된다")
    void save_keepsCoordinateScaleThroughDatabase() {
        ControlPoint saved = adapter.save(ControlPoint.register(
                "41192D000009998", PointType.DOGEUN, "스케일",
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                        new BigDecimal("545236.7712"), new BigDecimal("181840.9605")),
                new GeoCoordinate(126.794623, 37.506423),
                null, null, null, null, null, null, null, null, null, null));
        repository.flush();

        ControlPoint found = adapter.findById(saved.getId()).orElseThrow();

        // compareTo는 스케일을 무시하므로 equals로 자릿수까지 본다 — 컬럼 scale이 짧으면 여기서 걸린다
        assertEquals(new BigDecimal("545236.7712"), found.getTm().northing());
        assertEquals(new BigDecimal("181840.9605"), found.getTm().easting());
    }

    @Test
    @DisplayName("이름이나 관리번호가 겹치는 점을 한 번에 조회한다 — 임포트가 행마다 찾지 않도록")
    void findAllByNameInOrPointNoIn_returnsMatchingPoints() {
        adapter.save(csvRow1());
        adapter.save(ControlPoint.register(
                "41192D000001267", PointType.DOGEUN, "1466공",
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                        new BigDecimal("545201.74"), new BigDecimal("181833.69")),
                new GeoCoordinate(126.794541, 37.506107),
                "10300", "춘의동", "경기도 부천시 춘의동 102-16",
                MarkerMaterial.STEEL, InstallType.INSTALLED, LocalDate.of(2018, 2, 21), null, null, null, null));

        // 이름으로 하나, 관리번호로 다른 하나 — 둘 다 걸린다
        List<ControlPoint> found =
                adapter.findAllByNameInOrPointNoIn(List.of("1465공", "없는점"), List.of("41192D000001267"));

        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(p -> "1465공".equals(p.getName())));
        assertTrue(found.stream().anyMatch(p -> "1466공".equals(p.getName())));
        assertTrue(adapter.findAllByNameInOrPointNoIn(List.of(), List.of()).isEmpty()); // 빈 목록은 질의하지 않는다
    }

    @Test
    @DisplayName("저장 후 관리번호로 조회하면 성과·속성이 보존된다 (BigDecimal은 값 기준 비교)")
    void saveAndFindByPointNo_preservesAttributes() {
        adapter.save(csvRow1());

        ControlPoint found = adapter.findByPointNo("41192D000001265").orElseThrow();

        assertNotNull(found.getId());
        assertEquals(PointType.DOGEUN, found.getType());
        assertEquals("1465공", found.getName());
        assertEquals(CoordinateSystem.GRS80_CENTRAL, found.getTm().crs());
        // DB numeric 스케일 정규화(545236.77 → 545236.770)가 있어도 값은 같아야 한다
        assertEquals(0, new BigDecimal("545236.77").compareTo(found.getTm().northing()));
        assertEquals(0, new BigDecimal("181840.96").compareTo(found.getTm().easting()));
        assertEquals(126.794623, found.getGeo().longitude());
        assertEquals(37.506423, found.getGeo().latitude());
        assertEquals("10300", found.getRegionCode());
        assertEquals(new TraverseInfo("1", null, null, false), found.getTraverse());
        assertEquals(LocalDate.of(2018, 2, 21), found.getInstalledDate()); // date 컬럼 — 하루 밀림 없음
    }

    @Test
    @DisplayName("id 조회·전체 조회·관리번호 존재 확인이 동작한다")
    void findByIdAndFindAllAndExists() {
        ControlPoint saved = adapter.save(csvRow1());

        assertTrue(adapter.findById(saved.getId()).isPresent());
        assertEquals(1, adapter.findAll().size());
        assertTrue(adapter.existsByPointNo("41192D000001265"));
        assertFalse(adapter.existsByPointNo("41192D999999999"));
    }

    @Test
    @DisplayName("일괄 저장은 저장된 개수만큼 id가 발급된 목록을 반환한다")
    void saveAll_returnsSavedList() {
        ControlPoint second = ControlPoint.register(
                "41192D000001267", PointType.DOGEUN, "1466공",
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                        new BigDecimal("545201.74"), new BigDecimal("181833.69")),
                new GeoCoordinate(126.794541, 37.506107),
                "10300", "춘의동", "경기도 부천시 춘의동 102-16",
                MarkerMaterial.STEEL, InstallType.INSTALLED, LocalDate.of(2018, 2, 21), null
        , null, null, null);

        List<ControlPoint> saved = adapter.saveAll(List.of(csvRow1(), second));

        assertEquals(2, saved.size());
        assertTrue(saved.stream().allMatch(p -> p.getId() != null));
    }

    @Test
    @DisplayName("같은 관리번호는 두 번 저장할 수 없다")
    void duplicatePointNo_rejected() {
        adapter.save(csvRow1());

        assertThrows(DataIntegrityViolationException.class, () -> {
            adapter.save(csvRow1());
            repository.flush(); // 유니크 제약은 flush 시점에 검증된다
        });
    }

    @Test
    @DisplayName("종류별 개수는 저장된 종류만 키로 담아 실제 건수를 센다")
    void countByType_groupsSavedTypes() {
        adapter.save(csvRow1()); // DOGEUN 1
        adapter.save(ControlPoint.register(
                "41192D000001267", PointType.DOGEUN, "1466공",
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                        new BigDecimal("545201.74"), new BigDecimal("181833.69")),
                new GeoCoordinate(126.794541, 37.506107),
                null, null, null, null, null, null, null
        , null, null, null)); // DOGEUN 2 — 같은 종류 다건이 합산되는지(종류별 1 반환 회귀 차단)
        adapter.save(ControlPoint.register(
                "41190A000000001", PointType.TRIANGULATION, "삼각1",
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                        new BigDecimal("545000.00"), new BigDecimal("181000.00")),
                new GeoCoordinate(126.79, 37.50),
                null, null, null, null, null, null, null
        , null, null, null));

        Map<PointType, Long> counts = adapter.countByType();

        assertEquals(2, counts.get(PointType.DOGEUN));
        assertEquals(1, counts.get(PointType.TRIANGULATION));
        assertEquals(2, counts.size()); // 없는 종류(삼각보조)는 키가 없다 — 0 채움은 서비스 몫
    }
}
