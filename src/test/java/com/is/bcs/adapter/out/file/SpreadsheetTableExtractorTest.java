package com.is.bcs.adapter.out.file;

import com.is.bcs.application.port.out.file.Table;
import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 파일 → 표 추출 검증.
 * 같은 내용을 CSV·XLSX로 만들어 두고, 어느 쪽으로 올려도 뒤 단계가 같은 표를 받는지 본다.
 */
class SpreadsheetTableExtractorTest {

    private final SpreadsheetTableExtractor extractor = new SpreadsheetTableExtractor();

    private byte[] resource(String name) throws Exception {
        try (var in = getClass().getResourceAsStream(name)) {
            return in.readAllBytes();
        }
    }

    @Test
    @DisplayName("EUC-KR CSV 에서 헤더 22열과 데이터 49행을 뽑는다")
    void extract_eucKrCsv() throws Exception {
        Table table = extractor.extract(resource("/survey-target-sample.csv"));

        assertEquals(22, table.headers().size());
        assertEquals("순번", table.headers().getFirst());
        assertEquals("위도(Y)", table.headers().getLast());
        assertEquals(49, table.rows().size());
        assertEquals("41192D000001265", table.rows().getFirst().get(table.headers().indexOf("기준점번호")));
    }

    @Test
    @DisplayName("UTF-8 BOM CSV 도 같은 표로 읽히고 헤더에 BOM 이 섞이지 않는다")
    void extract_utf8BomCsv() throws Exception {
        Table table = extractor.extract(resource("/survey-target-utf8bom.csv"));

        assertEquals("순번", table.headers().getFirst());
        assertEquals(49, table.rows().size());
    }

    @Test
    @DisplayName("XLSX 도 CSV 와 같은 표로 읽힌다 — 날짜는 시리얼이 아닌 날짜 문자열, 좌표는 자릿수 그대로")
    void extract_xlsx() throws Exception {
        Table csv = extractor.extract(resource("/survey-target-sample.csv"));
        Table xlsx = extractor.extract(resource("/survey-target-sample.xlsx"));

        assertEquals(csv.headers(), xlsx.headers());
        assertEquals(csv.rows().size(), xlsx.rows().size());

        List<String> first = xlsx.rows().getFirst();
        assertEquals("545236.77", first.get(xlsx.headers().indexOf("X좌표")));
        assertEquals("181840.96", first.get(xlsx.headers().indexOf("Y좌표")));
        assertEquals("2018-02-21", first.get(xlsx.headers().indexOf("설치일자")));
        assertEquals("2025-09-08", first.get(xlsx.headers().indexOf("기존조사일")));
    }

    @Test
    @DisplayName("헤더가 첫 행이 아닌 XLSX 는 제목·빈 행을 건너뛰고 헤더 행을 찾는다")
    void extract_xlsxWithTitleRows() throws Exception {
        Table plain = extractor.extract(resource("/survey-target-sample.xlsx"));
        Table titled = extractor.extract(resource("/survey-target-titled.xlsx"));

        assertEquals(plain.headers(), titled.headers());
        assertEquals(plain.rows().size(), titled.rows().size());
    }

    @Test
    @DisplayName("빈 파일과 헤더 없는 파일은 각각의 사유로 거부한다")
    void extract_emptyAndHeaderless() {
        assertThrows(InvalidControlPointException.class, () -> extractor.extract(new byte[0]));
        assertThrows(InvalidControlPointException.class, () -> extractor.extract(null));

        // 줄바꿈뿐이라 채워진 칸이 하나도 없다 — 헤더로 삼을 행이 없다
        InvalidControlPointException thrown = assertThrows(InvalidControlPointException.class,
                () -> extractor.extract("\n\n".getBytes(StandardCharsets.UTF_8)));
        assertTrue(thrown.getMessage().contains("헤더"), thrown.getMessage());
    }

    @Test
    @DisplayName("따옴표 안의 콤마·줄바꿈·겹따옴표를 값으로 보존한다")
    void extract_csvQuoting() {
        String csv = "이름,주소,비고\n"
                + "\"가,나\",\"춘의동\n102-16\",\"큰 \"\"돌\"\" 옆\"\n";

        Table table = extractor.extract(csv.getBytes(StandardCharsets.UTF_8));

        assertEquals(List.of("이름", "주소", "비고"), table.headers());
        assertEquals(1, table.rows().size());
        assertEquals(List.of("가,나", "춘의동\n102-16", "큰 \"돌\" 옆"), table.rows().getFirst());
    }

    @Test
    @DisplayName("따옴표가 닫히지 않은 CSV 는 거부한다 — 뒷줄이 한 값으로 합쳐져 행 수가 줄어든다")
    void extract_unclosedQuote_isRejected() {
        String csv = "이름,주소\n"
                + "1465공,\"춘의동\n"
                + "1466공,상동\n";

        InvalidControlPointException thrown = assertThrows(InvalidControlPointException.class,
                () -> extractor.extract(csv.getBytes(StandardCharsets.UTF_8)));

        assertTrue(thrown.getMessage().contains("따옴표"), thrown.getMessage());
    }

    @Test
    @DisplayName("헤더 뒤쪽의 빈 칸은 열로 세지 않는다 — 엑셀이 남긴 빈 셀")
    void extract_trailingEmptyHeaders() {
        Table table = extractor.extract("이름,주소,,\n가,나,,\n".getBytes(StandardCharsets.UTF_8));

        assertEquals(List.of("이름", "주소"), table.headers());
        assertEquals(List.of("가", "나"), table.rows().getFirst());
    }

    @Test
    @DisplayName("XLSX 의 참·거짓 셀·수식 셀·오류 셀도 문자열로 읽는다")
    void extract_xlsxCellTypes() throws Exception {
        byte[] xlsx = workbookBytes(sheet -> {
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("이름");
            header.createCell(1).setCellValue("대상");
            header.createCell(2).setCellValue("합");
            header.createCell(3).setCellValue("오류");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("1465공");
            row.createCell(1).setCellValue(true);
            row.createCell(2).setCellFormula("1+2");
            row.createCell(3).setCellErrorValue(FormulaError.DIV0.getCode());
        });

        Table table = extractor.extract(xlsx);

        // 수식은 계산된 결과를, 읽을 수 없는 셀(오류)은 빈 값을 준다 — 뒤 단계는 언제나 문자열만 본다
        assertEquals(List.of("1465공", "true", "3", ""), table.rows().getFirst());
    }

    @Test
    @DisplayName("헤더로 삼을 행이 없는 XLSX 는 거부한다 — 한 칸짜리 제목만 있는 파일")
    void extract_xlsxWithoutHeaderRow() throws Exception {
        byte[] xlsx = workbookBytes(sheet -> sheet.createRow(0).createCell(0).setCellValue("부천시 지적기준점"));

        InvalidControlPointException thrown =
                assertThrows(InvalidControlPointException.class, () -> extractor.extract(xlsx));

        assertTrue(thrown.getMessage().contains("헤더"), thrown.getMessage());
    }

    @Test
    @DisplayName("시트가 하나도 없는 XLSX 는 그 사유로 거부한다")
    void extract_xlsxWithoutSheet() throws Exception {
        byte[] xlsx;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            workbook.write(out);
            xlsx = out.toByteArray();
        }

        InvalidControlPointException thrown =
                assertThrows(InvalidControlPointException.class, () -> extractor.extract(xlsx));

        assertTrue(thrown.getMessage().contains("시트"), thrown.getMessage());
    }

    /**
     * 테스트용 xlsx 를 메모리에서 만든다 — 픽스처 파일을 늘리지 않고 셀 종류만 확인하려는 목적.
     * 수식은 미리 계산해 결과를 캐시에 넣는다(엑셀이 저장할 때 하는 일과 같다).
     */
    private byte[] workbookBytes(java.util.function.Consumer<Sheet> fill) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            fill.accept(workbook.createSheet("대상지"));
            XSSFFormulaEvaluator.evaluateAllFormulaCells(workbook);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName("옛 엑셀 형식(.xls)도 같은 표로 읽는다 — 담당자가 어떤 형식으로 저장하든 통과한다")
    void extract_oldExcelFormat() throws Exception {
        byte[] xls;
        try (HSSFWorkbook workbook = new HSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("대상지");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("기준점명");
            header.createCell(1).setCellValue("X좌표");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("1465공");
            row.createCell(1).setCellValue(545236.77);
            workbook.write(out);
            xls = out.toByteArray();
        }

        Table table = extractor.extract(xls);

        assertEquals(List.of("기준점명", "X좌표"), table.headers());
        assertEquals(List.of("1465공", "545236.77"), table.rows().getFirst());
    }

    @Test
    @DisplayName("암호가 걸린 xlsx 는 암호를 풀라고 알린다 — zip 이 아니라 OLE2 로 저장돼도 옛 형식으로 오인하지 않는다")
    void extract_encryptedXlsx_isRejectedWithPasswordMessage() throws Exception {
        byte[] encrypted;
        try (POIFSFileSystem fs = new POIFSFileSystem();
             XSSFWorkbook workbook = new XSSFWorkbook();
             var out = new ByteArrayOutputStream()) {
            workbook.createSheet("대상지").createRow(0).createCell(0).setCellValue("기준점명");
            Encryptor encryptor = new EncryptionInfo(EncryptionMode.agile).getEncryptor();
            encryptor.confirmPassword("1234");
            try (var stream = encryptor.getDataStream(fs)) {
                workbook.write(stream);
            }
            fs.writeFilesystem(out);
            encrypted = out.toByteArray();
        }

        InvalidControlPointException thrown =
                assertThrows(InvalidControlPointException.class, () -> extractor.extract(encrypted));

        assertTrue(thrown.getMessage().contains("암호"), thrown.getMessage());
    }

    @Test
    @DisplayName("열리지 않는 엑셀 파일은 도메인 오류로 거부한다 — 서버 오류로 새지 않게")
    void extract_brokenXlsx_isRejected() {
        // zip 서명만 맞고 내용은 엑셀이 아니다 — POI 가 런타임 예외를 던지는 자리
        byte[] broken = {0x50, 0x4B, 0x03, 0x04, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05};

        assertThrows(InvalidControlPointException.class, () -> extractor.extract(broken));
    }
}
