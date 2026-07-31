package com.is.bcs.application.port.out.file;

import java.util.List;

/**
 * 파일에서 뽑아낸 표 — 헤더 한 줄과 그 아래 데이터 행들.
 * 이 형태로 바꾸고 나면 뒤 단계는 원본이 CSV였는지 XLSX였는지 몰라도 된다.
 * 값은 전부 문자열이다 — 숫자 서식 때문에 좌표 자릿수가 잘리거나 날짜가 시리얼로 바뀌는 것을 막는다.
 */
public record Table(List<String> headers, List<List<String>> rows) {
}
