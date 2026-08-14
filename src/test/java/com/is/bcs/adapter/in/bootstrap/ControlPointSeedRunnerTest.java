package com.is.bcs.adapter.in.bootstrap;

import com.is.bcs.adapter.out.file.SpreadsheetTableExtractor;
import com.is.bcs.adapter.out.geo.Proj4jCoordinateTransformer;
import com.is.bcs.application.service.ControlPointFileMapper;
import com.is.bcs.application.service.ControlPointImportService;
import com.is.bcs.application.service.ControlPointRegistrar;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.ServiceArea;
import com.is.bcs.support.FakeControlPointStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 기동 시드 검증 — 도근점 성과와 고객사 정리분을 함께 넣고, 읽지 못한 행은 건너뛴다. */
class ControlPointSeedRunnerTest {

    private static final int DOGEUN_SEED_POINTS = 2146;

    private final FakeControlPointStore store = new FakeControlPointStore();

    /** 도근점 성과를 넣을지는 설정이 가른다 — 두 경우를 다 세워 본다. */
    private ControlPointSeedRunner runner(boolean dogeunEnabled) {
        return new ControlPointSeedRunner(
                new ControlPointImportService(
                        new SpreadsheetTableExtractor(),
                        new ControlPointFileMapper(new Proj4jCoordinateTransformer()),
                        new ControlPointRegistrar(store, store),
                        directTransaction(), store, store),
                dogeunEnabled);
    }

    private final ControlPointSeedRunner runner = runner(true);

    @Test
    @DisplayName("기준점이 하나도 없으면 도근점 성과와 고객사 파일을 함께 등록한다")
    void run_emptyTable_seeds() {
        runner.run();

        // 고객사 파일에는 도근점에 없던 삼각보조점이 들어 있어, 두 파일이 모두 읽혔는지 종류로 확인된다
        assertTrue(store.count() > DOGEUN_SEED_POINTS, "고객사 파일이 등록되지 않았습니다");
        assertTrue(store.countByType().getOrDefault(PointType.TRIANGULATION_AUX, 0L) > 0);
        // 삼각보조점은 지역좌표 파일에도 있어 위 단언만으로는 세계좌표 파일 누락을 못 잡는다 —
        // 세계 좌표계 점은 그 파일에서만 오므로 존재 여부로 가른다
        assertTrue(store.findAll().stream().anyMatch(p -> p.getTm().crs() == CoordinateSystem.GRS80_CENTRAL),
                "세계좌표 파일이 등록되지 않았습니다");
    }

    @Test
    @DisplayName("관리 지역을 벗어난 좌표도 등록한다 — 막지 않고 알리기만 한다")
    void run_rowOutsideServiceArea_stillSeeded() {
        runner.run();

        // 이 점은 좌표계구분이 '지역'인데 성과는 세계측지계 값이라, 변환하면 부천에서 90km 넘게 벗어난다.
        // 값이 맞는지는 성과를 가진 쪽이 판단할 일이라 등록은 하고 알리기만 한다.
        ControlPoint outside = store.findByNameAndType("5673", PointType.DOGEUN).orElseThrow();
        assertFalse(ServiceArea.BUCHEON.contains(outside.getGeo()));
    }

    @Test
    @DisplayName("도근점 성과를 꺼 두면 고객사 파일만 등록한다 — 파일은 그대로 두고 넣지 않을 뿐이다")
    void run_dogeunDisabled_seedsCustomerFilesOnly() {
        runner(false).run();

        // 고객사 파일은 백여 행이고 도근점 성과는 2,146행이다. 성과가 함께 들어갔다면 개수가 그 수를 넘으므로
        // 관리번호 하나로 가리지 않고 개수로 가른다 — 두 파일에 같은 관리번호가 서른둘 겹쳐 있어 한 점의 존재로는 못 가린다
        assertTrue(store.count() > 0, "고객사 파일이 등록되지 않았습니다");
        assertTrue(store.count() < DOGEUN_SEED_POINTS, "도근점 성과가 함께 등록됐습니다");
        assertTrue(store.countByType().getOrDefault(PointType.TRIANGULATION_AUX, 0L) > 0);
    }

    @Test
    @DisplayName("기준점이 이미 있으면 아무것도 등록하지 않는다")
    void run_nonEmptyTable_skips() {
        ControlPoint first = DogeunSeedCsv.load().getFirst();
        store.save(first);

        runner.run();

        assertEquals(1, store.count());
    }

    /** 페이크 저장소에는 트랜잭션이 없다 — 경계만 통과시키고 커밋·롤백은 하지 않는다. */
    private static TransactionTemplate directTransaction() {
        return new TransactionTemplate(new PlatformTransactionManager() {

            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        });
    }
}
