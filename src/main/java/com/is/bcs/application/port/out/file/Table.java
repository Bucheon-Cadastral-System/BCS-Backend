package com.is.bcs.application.port.out.file;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 파일에서 뽑아낸 표 — 헤더 한 줄과 그 아래 데이터 행들.
 * 이 형태로 바꾸고 나면 뒤 단계는 원본이 CSV였는지 XLSX였는지 몰라도 된다.
 * 값은 전부 문자열이다 — 숫자 서식 때문에 좌표 자릿수가 잘리거나 날짜가 시리얼로 바뀌는 것을 막는다.
 *
 * @param sourceRowNumbers 각 데이터 행이 원본 파일의 몇 번째 줄이었는지(1부터).
 *                         제목 행·빈 줄을 건너뛰고 헤더를 찾으므로 순서만으로는 원본 위치를 알 수 없고,
 *                         오류 메시지의 행 번호가 어긋나면 담당자가 엉뚱한 줄을 고치게 된다.
 */
public record Table(List<String> headers, List<List<String>> rows, List<Integer> sourceRowNumbers) {

    public Table {
        if (rows.size() != sourceRowNumbers.size()) {
            throw new IllegalArgumentException("행 수와 원본 행 번호 수가 다릅니다.");
        }
    }

    /** 원본 위치를 따로 모를 때 — 헤더가 1행이고 데이터가 그 다음 줄부터 이어진다고 본다. */
    public Table(List<String> headers, List<List<String>> rows) {
        this(headers, rows, IntStream.rangeClosed(2, rows.size() + 1).boxed().toList());
    }
}
