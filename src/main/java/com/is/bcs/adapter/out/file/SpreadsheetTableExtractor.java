package com.is.bcs.adapter.out.file;

import com.is.bcs.application.port.out.file.Table;
import com.is.bcs.application.port.out.file.TableExtractor;
import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
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
import java.text.Normalizer;
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
    /** OLE2 복합 문서 서명 — 옛 엑셀(.xls)과 **암호가 걸린 xlsx** 가 이 형식이다. */
    private static final byte[] OLE2_MAGIC =
            {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
    /** 눈에 보이지 않는 문자라 리터럴로 두면 편집·저장 과정에서 조용히 훼손될 수 있다. */
    private static final String UTF8_BOM = "\uFEFF";
    /** 제목 행은 보통 한 칸만 채워져 있어, 두 칸 이상 채워진 첫 행을 헤더로 본다. */
    private static final int HEADER_MIN_FILLED_CELLS = 2;

    static {
        // xlsx는 zip이라 작은 파일이 수 GB로 풀리는 압축 폭탄이 가능하다.
        // POI의 비율 방어(기본 1:100)에 절대 상한을 더해, 업로드 상한(20MB)짜리 파일이 힙을 다 먹기 전에 끊는다.
        ZipSecureFile.setMaxEntrySize(64L * 1024 * 1024);
    }

    @Override
    public Table extract(byte[] content) {
        if (content == null || content.length == 0) {
            throw new InvalidControlPointException("빈 파일입니다.");
        }
        // 통합 문서인지는 POI 가 판단한다 — 암호가 걸린 xlsx 는 zip 이 아니라 OLE2 로 저장되므로
        // 서명만 보고 옛 형식이라 단정하면 "암호를 푸세요" 대신 엉뚱한 안내가 나간다
        boolean workbook = startsWith(content, ZIP_MAGIC) || startsWith(content, OLE2_MAGIC);
        return workbook ? fromWorkbook(content) : fromCsv(decode(content));
    }

    /**
     * 맥에서 저장한 파일은 한글이 자모 분리형(NFD)일 수 있다 — 화면에는 결합형과 똑같이 보이지만 문자열이 달라
     * 헤더 인식·이름/종류 매칭·검색이 조용히 어긋난다(파일명 fileBaseName 을 NFC 로 맞춘 것과 같은 이유).
     * 문자가 들어오는 길목(CSV 디코딩·셀 읽기)에서 결합형(NFC)으로 맞춰, 표에 실리는 문자열은 전부 NFC 가 되게 한다.
     */
    private static String nfc(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    private static boolean startsWith(byte[] content, byte[] magic) {
        if (content.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (content[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    /** UTF-8 로 읽히면 UTF-8, 깨지면 CP949 로 본다. 한글이 CP949 로 저장되면 UTF-8 디코딩에서 반드시 실패한다. */
    private static String decode(byte[] content) {
        String text;
        try {
            String decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
            text = decoded.startsWith(UTF8_BOM) ? decoded.substring(1) : decoded;
        } catch (CharacterCodingException e) {
            // CP949 에는 결합형 음절만 있어 아래 정규화가 건드릴 것이 없다 — 해도 무해해서 경로를 가르지 않는다
            text = new String(content, CP949);
        }
        return nfc(text);
    }

    private static Table fromCsv(String text) {
        List<CsvRecord> records = splitCsv(text);

        // 헤더 찾는 규칙은 XLSX 와 같다 — 같은 양식을 CSV 로 내보내도 제목 행이 첫 줄에 남는다
        int headerAt = 0;
        while (headerAt < records.size() && filledCount(records.get(headerAt).values()) < HEADER_MIN_FILLED_CELLS) {
            headerAt++;
        }
        if (headerAt == records.size()) {
            throw new InvalidControlPointException("파일에 헤더가 없습니다.");
        }

        List<String> headers = trimTrailingEmpty(records.get(headerAt).values());
        List<List<String>> rows = new ArrayList<>();
        List<Integer> rowNumbers = new ArrayList<>();
        for (CsvRecord record : records.subList(headerAt + 1, records.size())) {
            rows.add(fit(record.values(), headers.size()));
            rowNumbers.add(record.line());
        }
        return new Table(headers, rows, rowNumbers);
    }

    private static Table fromWorkbook(byte[] content) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new InvalidControlPointException("엑셀 파일에 시트가 없습니다.");
            }
            // 첫 시트만 읽는다 — 대상지는 한 장에 담기는 표라 여러 장 중 어느 것인지 묻는 단계를 두지 않는다
            Sheet sheet = workbook.getSheetAt(0);

            List<String> headers = null;
            List<List<String>> rows = new ArrayList<>();
            List<Integer> rowNumbers = new ArrayList<>();
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
                    rowNumbers.add(row.getRowNum() + 1);
                }
            }
            if (headers == null) {
                throw new InvalidControlPointException("파일에 헤더가 없습니다.");
            }
            return new Table(headers, rows, rowNumbers);
        } catch (InvalidControlPointException e) {
            throw e;
        } catch (EncryptedDocumentException e) {
            throw new InvalidControlPointException("암호가 걸린 엑셀 파일은 읽을 수 없습니다. 암호를 풀고 올려 주세요.", e);
        } catch (IOException | RuntimeException e) {
            // POI 는 열지 못한 파일을 형식마다 다른 런타임 예외로 알린다 — 어느 쪽이든 잘못된 업로드이지 서버 오류가 아니다.
            // 다만 여기에 우리 결함이 섞여 들어올 수 있으므로 원인은 버리지 않고 함께 남긴다.
            throw new InvalidControlPointException("엑셀 파일을 읽지 못했습니다. 파일이 손상되지 않았는지 확인해 주세요.", e);
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
            case STRING -> nfc(cell.getStringCellValue()).trim();
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
    /** 한 레코드와 그것이 시작한 원본 줄 번호(1부터). 값 안의 줄바꿈 때문에 레코드 수와 줄 수가 다르다. */
    private record CsvRecord(int line, List<String> values) {
    }

    private static List<CsvRecord> splitCsv(String text) {
        List<CsvRecord> records = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int line = 1;
        int recordLine = 1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c != '"') {
                    if (c == '\n') {
                        line++; // 값 안의 줄바꿈도 파일에서는 한 줄이다
                    }
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
                    records.add(new CsvRecord(recordLine, List.copyOf(current)));
                }
                current.clear();
                line++;
                recordLine = line;
            } else {
                field.append(c);
            }
        }

        if (quoted) {
            // 따옴표를 닫지 않으면 그 뒤 줄들이 한 값으로 합쳐져 행 수가 조용히 줄어든다
            throw new InvalidControlPointException("따옴표가 닫히지 않았습니다. 파일이 온전한지 확인해 주세요.");
        }

        current.add(field.toString().trim());
        if (filledCount(current) > 0) {
            records.add(new CsvRecord(recordLine, List.copyOf(current)));
        }
        return records;
    }
}
