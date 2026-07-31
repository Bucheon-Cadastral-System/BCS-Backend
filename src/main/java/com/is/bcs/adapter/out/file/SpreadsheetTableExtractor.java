package com.is.bcs.adapter.out.file;

import com.is.bcs.application.port.out.file.Table;
import com.is.bcs.application.port.out.file.TableExtractor;
import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 업로드 파일에서 표를 뽑는다 — CSV(인코딩 자동 감지)와 XLSX 를 같은 형태로 만든다.
 * 서식 해석은 여기서 끝내고 값은 전부 문자열로 넘긴다: 숫자로 넘기면 좌표 자릿수가 흔들리고 날짜가 시리얼이 된다.
 */
@Component
public class SpreadsheetTableExtractor implements TableExtractor {

    /** 관공서 내보내기가 흔히 쓰는 인코딩. CP949 는 EUC-KR 을 포함하므로 이것 하나로 둘 다 읽힌다. */
    private static final Charset CP949 = Charset.forName("MS949");
    private static final byte[] ZIP_MAGIC = {0x50, 0x4B, 0x03, 0x04}; // xlsx 는 zip 이다
    private static final String UTF8_BOM = "﻿";
    /** 제목 행은 보통 한 칸만 채워져 있어, 두 칸 이상 채워진 첫 행을 헤더로 본다. */
    private static final int HEADER_MIN_FILLED_CELLS = 2;

    @Override
    public Table extract(byte[] content) {
        if (content == null || content.length == 0) {
            throw new InvalidControlPointException("빈 파일입니다.");
        }
        return isZip(content) ? fromXlsx(content) : fromCsv(decode(content));
    }

    private static boolean isZip(byte[] content) {
        if (content.length < ZIP_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < ZIP_MAGIC.length; i++) {
            if (content[i] != ZIP_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    /** UTF-8 로 읽히면 UTF-8, 깨지면 CP949 로 본다. 한글이 CP949 로 저장되면 UTF-8 디코딩에서 반드시 실패한다. */
    private static String decode(byte[] content) {
        try {
            String decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
            return decoded.startsWith(UTF8_BOM) ? decoded.substring(1) : decoded;
        } catch (CharacterCodingException e) {
            return new String(content, CP949);
        }
    }

    private static Table fromCsv(String text) {
        List<List<String>> records = splitCsv(text);
        if (records.isEmpty()) {
            throw new InvalidControlPointException("파일에 헤더가 없습니다.");
        }
        List<String> headers = trimTrailingEmpty(records.getFirst());
        List<List<String>> rows = new ArrayList<>();
        for (List<String> record : records.subList(1, records.size())) {
            rows.add(fit(record, headers.size()));
        }
        return new Table(headers, rows);
    }

    private static Table fromXlsx(byte[] content) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);

            List<String> headers = null;
            List<List<String>> rows = new ArrayList<>();
            for (Row row : sheet) {
                List<String> values = readRow(row);
                if (headers == null) {
                    // 제목 행·빈 행을 지나 실제 헤더 행을 찾는다
                    if (filledCount(values) >= HEADER_MIN_FILLED_CELLS) {
                        headers = trimTrailingEmpty(values);
                    }
                    continue;
                }
                if (filledCount(values) > 0) {
                    rows.add(fit(values, headers.size()));
                }
            }
            if (headers == null) {
                throw new InvalidControlPointException("파일에 헤더가 없습니다.");
            }
            return new Table(headers, rows);
        } catch (IOException e) {
            throw new InvalidControlPointException("엑셀 파일을 읽지 못했습니다.");
        }
    }

    private static List<String> readRow(Row row) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            values.add(readCell(row.getCell(i)));
        }
        return values;
    }

    private static String readCell(Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (type) {
            case STRING -> cell.getStringCellValue().trim();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    // 지수 표기·불필요한 0 없이 적힌 그대로 — 좌표는 소수 자릿수가 성과의 정밀도다
                    : BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            default -> "";
        };
    }

    private static int filledCount(List<String> values) {
        return (int) values.stream().filter(v -> !v.isEmpty()).count();
    }

    /** 헤더 뒤쪽의 빈 칸은 열이 아니다 — 엑셀은 건드린 적 있는 칸을 빈 셀로 남긴다. */
    private static List<String> trimTrailingEmpty(List<String> values) {
        int end = values.size();
        while (end > 0 && values.get(end - 1).isEmpty()) {
            end--;
        }
        return List.copyOf(values.subList(0, end));
    }

    /** 행을 헤더 길이에 맞춘다 — 짧으면 빈 값으로 채우고, 길면 잘라 열 위치가 어긋나지 않게 한다. */
    private static List<String> fit(List<String> values, int size) {
        List<String> fitted = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            fitted.add(i < values.size() ? values.get(i) : "");
        }
        return List.copyOf(fitted);
    }

    /**
     * 따옴표 안의 콤마와 줄바꿈을 보존하는 CSV 분해.
     * 줄 단위로 자르면 값 안에 줄바꿈이 든 행이 두 행으로 갈라지므로 문자 단위로 훑는다.
     */
    private static List<List<String>> splitCsv(String text) {
        List<List<String>> records = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c != '"') {
                    field.append(c);
                } else if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = false;
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                current.add(field.toString().trim());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                current.add(field.toString().trim());
                field.setLength(0);
                if (filledCount(current) > 0) {
                    records.add(List.copyOf(current));
                }
                current.clear();
            } else {
                field.append(c);
            }
        }

        current.add(field.toString().trim());
        if (filledCount(current) > 0) {
            records.add(List.copyOf(current));
        }
        return records;
    }
}
