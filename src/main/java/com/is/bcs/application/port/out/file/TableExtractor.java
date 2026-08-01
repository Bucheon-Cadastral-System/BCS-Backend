package com.is.bcs.application.port.out.file;

/** 업로드한 파일에서 표를 뽑는다. 형식은 파일 내용으로 판별한다 — 확장자를 바꿔 올려도 읽히게. */
public interface TableExtractor {

    Table extract(byte[] content);
}
