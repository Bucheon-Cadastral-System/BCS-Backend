package com.is.bcs.application.service;

import com.is.bcs.adapter.out.file.SpreadsheetTableExtractor;
import com.is.bcs.adapter.out.geo.Proj4jCoordinateTransformer;
import com.is.bcs.application.dto.ControlPointImportResult;
import com.is.bcs.application.dto.ControlPointSeedResult;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import com.is.bcs.support.FakeControlPointStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 기준점 파일 등록 검증 — 조사를 만들지 않고 마스터만 반영한다. */
class ControlPointImportServiceTest {

    /** 고객사가 정리해 준 파일 — 107행 중 한 행은 좌표계구분과 성과가 어긋나 있다. */
    private static final String CUSTOMER_FILE = "/seed/control-points-bucheon-bessel.xlsx";

    private final FakeControlPointStore store = new FakeControlPointStore();
    private final ControlPointImportService service = new ControlPointImportService(
            new SpreadsheetTableExtractor(),
            new ControlPointFileMapper(new Proj4jCoordinateTransformer()),
            new ControlPointRegistrar(store, store),
            directTransaction());

    private byte[] read(String resource) throws Exception {
        try (var in = getClass().getResourceAsStream(resource)) {
            return in.readAllBytes();
        }
    }

    @Test
    @DisplayName("실파일 49행이 모두 신규 기준점으로 등록된다")
    void importControlPoints_registersEveryRow() throws Exception {
        ControlPointImportResult result = service.importControlPoints(read("/survey-target-sample.csv"));

        assertEquals(49, result.totalRows());
        assertEquals(49, result.newPoints());
        assertEquals(0, result.updatedPoints());
        assertEquals(49, store.count());
    }

    @Test
    @DisplayName("같은 파일을 다시 올리면 새로 등록하지 않고 기존 점으로 센다")
    void importControlPoints_sameFileTwice_reusesPoints() throws Exception {
        service.importControlPoints(read("/survey-target-sample.csv"));

        ControlPointImportResult second = service.importControlPoints(read("/survey-target-sample.csv"));

        assertEquals(0, second.newPoints());
        assertEquals(49, second.existingPoints());
        assertEquals(49, store.count());
    }

    @Test
    @DisplayName("읽지 못한 행이 하나라도 있으면 아무것도 등록하지 않는다")
    void importControlPoints_rowError_rejectsWholeFile() {
        // 필수 열의 칸이 빈 행 — 등록 단계에서 터뜨리지 않고 읽는 단계에서 거른다
        byte[] file = "기준점번호,종류,기준점명,좌표계구분,X좌표,Y좌표\n,도근점,1465공,세계,545236.77,181840.96\n"
                .getBytes(StandardCharsets.UTF_8);

        InvalidControlPointException e =
                assertThrows(InvalidControlPointException.class, () -> service.importControlPoints(file));

        assertTrue(e.getMessage().contains("기준점번호"), e.getMessage());
        assertEquals(0, store.count());
    }

    @Test
    // 경고 내용 자체는 시드 결과(seed_reportsRowsToCheck)와 미리보기 검증이 본다 — 여기서는 등록되는 것만 확인한다
    @DisplayName("관리 지역을 벗어난 좌표도 행이 거부되지 않고 등록된다")
    void importControlPoints_outsideServiceArea_registers() throws Exception {
        ControlPointImportResult result = service.importControlPoints(read(CUSTOMER_FILE));

        assertEquals(107, result.totalRows());
        assertTrue(store.findByNameAndType("5673", PointType.DOGEUN).isPresent());
    }

    @Test
    @DisplayName("시드는 넣은 뒤 확인이 필요한 행을 사유와 함께 알린다")
    void seed_reportsRowsToCheck() throws Exception {
        ControlPointSeedResult result = service.seed(read(CUSTOMER_FILE));

        assertEquals(107, result.seeded());
        assertTrue(result.skipped().isEmpty());
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().getFirst().startsWith("76행: "), result.warnings().getFirst());
        // 도근점만 있는 성과 파일과 달리 삼각보조점이 섞여 있다 — 종류 어휘가 그대로 읽혔는지 함께 본다
        assertEquals(4L, store.countByType().get(PointType.TRIANGULATION_AUX));
    }

    /** 페이크 저장소에는 트랜잭션이 없다 — 경계만 통과시키고 커밋·롤백은 하지 않는다. */
    @Test
    @DisplayName("최종조사 열이 있는 파일은 요약을 채우고, 그 열이 없는 파일이 와도 지우지 않는다")
    void importControlPoints_lastSurveyMergeRule() {
        String withLast = """
                기준점번호,종류,기준점명,좌표계구분,X좌표,Y좌표,최종조사내용,최종조사일자
                41192D000000001,도근점,1465공,세계,545236.77,181840.96,망실,2025-09-08
                """;
        service.importControlPoints(withLast.getBytes(StandardCharsets.UTF_8));
        ControlPoint stored = store.findByNameAndType("1465공", PointType.DOGEUN).orElseThrow();
        assertEquals("망실", stored.getLastSurveyResult());
        assertEquals(LocalDate.of(2025, 9, 8), stored.getLastSurveyedOn());

        String withoutLast = """
                기준점번호,종류,기준점명,좌표계구분,X좌표,Y좌표
                41192D000000001,도근점,1465공,세계,545236.77,181840.96
                """;
        ControlPointImportResult second = service.importControlPoints(withoutLast.getBytes(StandardCharsets.UTF_8));

        ControlPoint kept = store.findByNameAndType("1465공", PointType.DOGEUN).orElseThrow();
        assertEquals("망실", kept.getLastSurveyResult()); // 열이 없다 = 모른다 — 지우지 않는다
        assertEquals(LocalDate.of(2025, 9, 8), kept.getLastSurveyedOn());
        assertEquals(0, second.updatedPoints()); // 지워지지 않으므로 갱신도 아니다
    }

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
