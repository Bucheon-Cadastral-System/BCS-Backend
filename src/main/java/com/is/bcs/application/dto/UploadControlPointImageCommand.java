package com.is.bcs.application.dto;

import com.is.bcs.domain.survey.SurveyResult;

import java.time.OffsetDateTime;

/**
 * 기준점 현장 이미지 업로드 명령.
 *
 * pointName과 작성자 ID는 요청 본문에서 받지 않는다.
 * 기준점명은 DB에서, 작성자 ID는 인증 주체에서 가져온다.
 *
 * <p>판정({@code result})을 함께 받는다. 사진 한 장으로는 그 점이 정상인지 망실인지 알 수 없어
 * 서버가 대신 고를 수 없고, 사진만 남고 판정이 빠지면 조사일만 오늘로 바뀌어 "오늘 정상으로 판정했다"는
 * 거짓이 남는다. 현장에 다녀와 사진을 남기는 일과 판정하는 일은 한 동작이므로 한 요청으로 받는다.
 */
public record UploadControlPointImageCommand(
        Long projectId,
        Long pointId,
        String originalFileName,
        String contentType,
        long fileSize,
        byte[] content,
        OffsetDateTime capturedAt,
        SurveyResult result,
        String note,
        Long uploaderId
) {
}
