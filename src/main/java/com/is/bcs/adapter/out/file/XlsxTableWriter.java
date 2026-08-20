package com.is.bcs.adapter.out.file;

import com.is.bcs.application.port.out.file.Table;
import com.is.bcs.application.port.out.file.TableWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * 표를 xlsx 한 장으로 쓴다.
 *
 * <p>모든 칸을 문자열로 적는다. 좌표를 수로 적으면 엑셀이 자릿수를 줄여 성과가 달라지고, 날짜를 수로 적으면
 * 여는 컴퓨터의 표시 형식을 따라 다른 날짜로 보인다. 읽는 쪽({@link com.is.bcs.adapter.out.file.SpreadsheetTableExtractor})
 * 도 문자열로 받으므로 내보낸 파일을 그대로 다시 올릴 수 있다.
 *
 * <p>{@link SXSSFWorkbook} 은 쓴 행을 디스크로 흘려보내며 메모리에는 창만큼만 둔다 — 점 수천 개짜리 조사도
 * 한 번에 다 쥐지 않는다. 흘려 쓰기가 남긴 임시 파일은 close 가 지운다.
 */
@Component
public class XlsxTableWriter implements TableWriter {

    /** 메모리에 두는 행 수 — 이보다 오래된 행은 임시 파일로 내려간다. */
    private static final int WINDOW_ROWS = 200;
    /** 열 너비(문자 폭 × 256) — 자동 맞춤은 행을 다시 읽어야 해서 흘려 쓰기와 함께 쓸 수 없다. */
    private static final int COLUMN_WIDTH = 16 * 256;

    @Override
    public byte[] write(String sheetName, Table table) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(WINDOW_ROWS);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            writeRow(sheet, 0, table.headers(), headerStyle(workbook));
            List<List<String>> rows = table.rows();
            for (int i = 0; i < rows.size(); i++) {
                writeRow(sheet, i + 1, rows.get(i), null);
            }
            for (int column = 0; column < table.headers().size(); column++) {
                sheet.setColumnWidth(column, COLUMN_WIDTH);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("파일을 만들지 못했습니다.", e);
        }
    }

    private static void writeRow(Sheet sheet, int rowIndex, List<String> values, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.size(); i++) {
            Cell cell = row.createCell(i);
            String value = values.get(i);
            // 빈 칸은 값을 넣지 않는다 — 빈 문자열을 넣으면 엑셀이 '내용 있는 칸'으로 세어 마지막 열이 늘어난다
            if (value != null && !value.isEmpty()) {
                cell.setCellValue(value);
            }
            if (style != null) {
                cell.setCellStyle(style);
            }
        }
    }

    private static CellStyle headerStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font bold = workbook.createFont();
        bold.setBold(true);
        style.setFont(bold);
        return style;
    }
}
