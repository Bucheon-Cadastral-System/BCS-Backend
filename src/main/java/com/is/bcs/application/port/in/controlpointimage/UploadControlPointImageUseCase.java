package com.is.bcs.application.port.in.controlpointimage;

import com.is.bcs.application.dto.UploadControlPointImageCommand;
import com.is.bcs.application.dto.UploadControlPointImageResult;

public interface UploadControlPointImageUseCase {

    /**
     * 프로젝트의 기준점 현장 이미지를 등록하거나 교체한다.
     *
     * 기존 이미지가 없으면 생성하고, 있으면 새 파일 저장과 DB 변경이
     * 성공한 뒤 기존 파일을 제거한다.
     */
    UploadControlPointImageResult uploadOrReplace(UploadControlPointImageCommand command);

}