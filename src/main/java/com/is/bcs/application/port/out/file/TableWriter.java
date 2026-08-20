package com.is.bcs.application.port.out.file;

/**
 * 표를 스프레드시트 파일로 만든다 — {@link TableExtractor} 의 반대 방향이다.
 *
 * <p>읽는 쪽과 같은 {@link Table} 을 주고받으므로 응용 계층은 파일 형식을 모른다.
 * 값이 전부 문자열인 것도 같은 이유다. 좌표 자릿수와 날짜 표기를 적힌 그대로 내보낸다.
 */
public interface TableWriter {

    /**
     * @param sheetName 표가 담길 장의 이름
     * @return 파일 내용
     */
    byte[] write(String sheetName, Table table);
}
